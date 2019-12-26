package it.valeriovaudi.publisher.streaming

import org.reactivestreams.Publisher
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.lang.Exception
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue


class MetricEmitter(private val queues: ConcurrentLinkedQueue<ConcurrentLinkedQueue<Metric>>,
                    private val metricsRepository: MetricsRepository,
                    private val rabbitTemplate: RabbitTemplate ) {

    fun publish(metric: Metric) : Publisher<Void> {
        return metricsRepository.emit(metric)
                .toMono()
                .flatMap(this::sendToRabbit)
                .log()
                .then(Mono.empty())
    }

    fun subscribeOn(name: String): Publisher<Metric> {
        val queue = ConcurrentLinkedQueue<Metric>()
        queues.add(queue)
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
    }

    private fun sendToRabbit(metric: Metric): Mono<Metric> {
        return Mono.create<Metric> { emitter ->
            rabbitTemplate.convertAndSend("storeMetricsBus", metric)
            emitter.success()
        }.subscribeOn(Schedulers.elastic())
    }

    @RabbitListener(queues = ["storeMetricsBus"])
    fun listenToMetrics(metric: Metric) {
        println("metric: $metric")
        queues.forEach { queue -> queue.add(metric) }
    }
}