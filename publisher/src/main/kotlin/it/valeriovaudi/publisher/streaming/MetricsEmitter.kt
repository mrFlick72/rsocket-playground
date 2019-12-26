package it.valeriovaudi.publisher.streaming

import org.springframework.amqp.rabbit.annotation.RabbitListener
import reactor.core.publisher.Flux
import java.lang.Exception
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue


class MetricEmitter(private val queues: ConcurrentLinkedQueue<ConcurrentLinkedQueue<Metric>>) {

    fun store(metric: Metric) {
        queues.forEach { queue -> queue.add(metric) }
    }

    fun subscribeOn(name: String): Flux<Metric> {
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
}

class MetricsEmitterListener(private val emitter: MetricEmitter) {

    @RabbitListener(queues = ["storeMetricsBus"])
    fun listenToMetrics(metric: Metric) {
        println("metric: $metric")
        emitter.store(metric)
    }
}