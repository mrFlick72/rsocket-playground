package it.valeriovaudi.publisher

import com.rabbitmq.client.AMQP
import org.reactivestreams.Publisher
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.context.annotation.Bean
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.io.Serializable
import java.time.Duration

@SpringBootApplication
class PublisherApplication {

    @Bean
    fun jdbcMetricsRepository(databaseClient: DatabaseClient) = JdbcMetricsRepository(databaseClient)


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

@Controller
class RSocketMetricsEndPoint(private val metricsRepository: MetricsRepository,
                             private val rabbitTemplate: RabbitTemplate) {

    @MessageMapping("metrics/sse")
    fun sse(name: String): Publisher<Metric> = Flux.interval(Duration.ofSeconds(1)).flatMap { metricsRepository.findAll(name) }

    @MessageMapping("metrics/emit")
    fun emitter(metric: Metric): Publisher<Void> = metricsRepository.emit(metric)
            .toMono()
            .flatMap { metric: Metric ->
                Mono.create<Metric> { emitter ->
                    rabbitTemplate.convertAndSend("storeMetricsBus", metric)
                    emitter.success()
                }.subscribeOn(Schedulers.elastic())
            }
            .log()
            .then(Mono.empty())
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