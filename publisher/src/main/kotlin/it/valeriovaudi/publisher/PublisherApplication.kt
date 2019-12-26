package it.valeriovaudi.publisher

import org.reactivestreams.Publisher
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.io.Serializable
import java.lang.Exception
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue

@SpringBootApplication
class PublisherApplication {

    @Bean
    fun jdbcMetricsRepository(databaseClient: DatabaseClient) = JdbcMetricsRepository(databaseClient)

    @Bean
    fun metricEmitter() = MetricEmitter(ConcurrentLinkedQueue())

    @Bean("storeMetricsBus")
    fun storeMetricsBus(): Queue {
        return Queue("storeMetricsBus", false, false, false)
    }

    /*  @Bean
      fun storeMetrics(): Function<Metric, Mono<Metric>> =
              Function { metric: Metric -> Mono.just(metric) }

      @Bean
      fun publishMetrics(): Consumer<Mono<Metric>> =
              Consumer { metric: Mono<Metric> -> println("metric: $metric") }*/
}

fun main(args: Array<String>) {
    runApplication<PublisherApplication>(*args)
}

@Controller
class RSocketMessagingEndPoint {
    @MessageMapping("echo")
    fun echo(message: String): String {
        println(message)
        return "echo of message: $message"
    }
}

@Service
class MetricsEmitterListener(private val emitter: MetricEmitter) {

    @RabbitListener(queues = ["storeMetricsBus"])
    fun listenToMetrics(metric: Metric) {
        println("metric: $metric")
        emitter.store(metric)
    }
}

class MetricEmitter(private val queues: ConcurrentLinkedQueue<ConcurrentLinkedQueue<Metric>>) {

    fun store(metric: Metric) {
        queues.forEach { queue -> queue.add(metric) }
    }

    fun emit(): Flux<Metric> {
        val queue = ConcurrentLinkedQueue<Metric>()
        queues.add(queue)
        return Flux.interval(Duration.ofSeconds(1))
                .flatMap<Metric> {
                    try {
                        Flux.just(queue.remove())
                    } catch (e: Exception) {
                        Flux.empty<Metric>()
                    }
                }

    }
}

@Controller
class RSocketMetricsEndPoint(private val metricsRepository: MetricsRepository,
                             private val rabbitTemplate: RabbitTemplate,
                             private val emitter: MetricEmitter) {

    @MessageMapping("metrics/sse")
    fun sse(name: String): Publisher<Metric> = emitter.emit().filter {metric -> metric.name == name }

    @MessageMapping("metrics/emit")
    fun emitter(metric: Metric): Publisher<Void> = metricsRepository.emit(metric)
            .toMono()
            .flatMap(this::sendToRabbit)
            .log()
            .then(Mono.empty())

    private fun sendToRabbit(metric: Metric): Mono<Metric> {
        return Mono.create<Metric> { emitter ->
            rabbitTemplate.convertAndSend("storeMetricsBus", metric)
            emitter.success()
        }.subscribeOn(Schedulers.elastic())
    }
}

data class Metric(val name: String, val value: String) : Serializable

interface MetricsRepository {

    fun emit(metric: Metric): Publisher<Metric>

    fun findAll(name: String): Publisher<Metric>

}

class JdbcMetricsRepository(private val databaseClient: DatabaseClient) : MetricsRepository {
    override fun emit(metric: Metric): Publisher<Metric> =
            databaseClient.execute("INSERT INTO METRICS (METRIC_NAME, METRIC_VALUE) VALUES (:name, :value)")
                    .bind("name", metric.name)
                    .bind("value", metric.value)
                    .fetch()
                    .rowsUpdated()
                    .flatMap { Mono.just(metric) }


    override fun findAll(name: String): Publisher<Metric> =
            databaseClient.execute("SELECT METRIC_NAME, METRIC_VALUE FROM METRICS WHERE METRIC_NAME=:name")
                    .bind("name", name)
                    .fetch()
                    .all()
                    .map { sqlRowMap ->
                        Metric(
                                sqlRowMap.getValue("METRIC_NAME") as String,
                                sqlRowMap.getValue("METRIC_VALUE") as String
                        )
                    }


}