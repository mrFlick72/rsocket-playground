package it.valeriovaudi.publisher.streaming

import org.reactivestreams.Publisher
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.lang.Exception
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue


interface MetricsPublisher {
    fun publish(metric: Metric): Publisher<Void>
    fun subscribeOn(name: String): Publisher<Metric>
}

class RabbitMQMetricsPublisher(private val queues: ConcurrentLinkedQueue<ConcurrentLinkedQueue<Metric>>,
                               private val metricsRepository: MetricsRepository,
                               private val rabbitTemplate: RabbitTemplate) : MetricsPublisher {

    override fun publish(metric: Metric): Publisher<Void> =
            metricsRepository.emit(metric)
                    .toMono()
                    .flatMap(this::sendToRabbit)
                    .log()
                    .then(Mono.empty())

    override fun subscribeOn(name: String): Publisher<Metric> {
        val queue = ConcurrentLinkedQueue<Metric>()
        queues.add(queue)
        println("queues.size $queues")

        return Flux.interval(Duration.ofSeconds(1))
                .flatMap<Metric> {
                    emitMetricFrom(queue)
                }
                .filter { metric -> metric.name == name }
                .doOnCancel {
                    resourcesCleanUp(queue)
                }
                .doOnComplete {
                    resourcesCleanUp(queue)
                }
    }

    private fun emitMetricFrom(queue: ConcurrentLinkedQueue<Metric>): Flux<Metric> {
        return try {
            Flux.just(queue.remove())
        } catch (e: Exception) {
            Flux.empty<Metric>()
        }
    }

    private fun resourcesCleanUp(queue: ConcurrentLinkedQueue<Metric>) {
        val removedQueue = queues.remove(queue)
        println("queues.remove(queue): $removedQueue")
        println("queues.remove(queue): ${queues.size}")
    }

    private fun sendToRabbit(metric: Metric): Mono<Metric> {
        return Mono.create<Metric> { emitter ->
            rabbitTemplate.convertAndSend("storeMetricsExchange", "store-metrics", metric)
            emitter.success()
        }.subscribeOn(Schedulers.elastic())
    }

    @RabbitListener(queues = ["storeMetricsQueue"])
    fun listenToMetrics(metric: Metric) {
        println("metric: $metric")
        queues.forEach { queue -> queue.add(metric) }
    }
}