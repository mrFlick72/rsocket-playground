package it.valeriovaudi.publisher.streaming

import org.reactivestreams.Publisher
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers


@Controller
class RSocketMetricsEndPoint(private val metricsRepository: MetricsRepository,
                             private val rabbitTemplate: RabbitTemplate,
                             private val emitter: MetricEmitter) {

    @MessageMapping("metrics/sse")
    fun sse(name: String): Publisher<Metric> =
            emitter.emit()
                    .filter { metric -> metric.name == name }

    @MessageMapping("metrics/emit")
    fun emitter(metric: Metric): Publisher<Void> =
            metricsRepository.emit(metric)
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