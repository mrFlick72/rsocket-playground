package it.valeriovaudi.publisher.streaming

import org.reactivestreams.Publisher
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono


@Controller
class RSocketMetricsEndPoint(private val emitter: MetricEmitter) {

    @MessageMapping("metrics/sse")
    fun sse(name: String): Publisher<Metric> =
            emitter.subscribeOn(name)

    @MessageMapping("metrics/emit")
    fun emitter(metric: Metric): Publisher<Void> =
            emitter.publish(metric)

}