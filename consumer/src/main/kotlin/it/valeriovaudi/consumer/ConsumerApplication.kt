package it.valeriovaudi.consumer

import io.rsocket.RSocketFactory.ClientRSocketFactory
import io.rsocket.frame.decoder.PayloadDecoder
import io.rsocket.transport.netty.client.TcpClientTransport
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.util.MimeTypeUtils
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.awt.PageAttributes

@SpringBootApplication
class ConsumerApplication {

    @Bean
    fun rSocketRequester(rSocketStrategies: RSocketStrategies?): RSocketRequester? {
        return RSocketRequester.builder()
                .rsocketFactory { factory: ClientRSocketFactory ->
                    factory
                            .dataMimeType(MimeTypeUtils.ALL_VALUE)
                            .frameDecoder(PayloadDecoder.ZERO_COPY)
                            .transport(TcpClientTransport.create(7000))
                }
                .rsocketStrategies(rSocketStrategies)
                .connect(TcpClientTransport.create(7000))
                .retry()
                .block()
    }
}

fun main(args: Array<String>) {
    runApplication<ConsumerApplication>(*args)
}

data class Metric(val name: String, val value: String)

@Configuration
class ConsumerMessagesRoute(private val rSocketRequester: RSocketRequester) {

    @Bean
    fun routes() =
            router {
                GET("/echo/{message}")
                {
                    rSocketRequester.route("echo")
                            .data(it.pathVariable("message"))
                            .retrieveMono(String::class.java)
                            .toMono()
                            .flatMap {
                                ServerResponse.ok().body(BodyInserters.fromValue(it))
                            }
                }

                GET("/metrics/{name}")
                {
                    val retrieveFlux = rSocketRequester.route("metrics/sse")
                            .data(it.pathVariable("name"))
                            .retrieveFlux(Metric::class.java);


                    ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_STREAM_JSON)
                            .body(retrieveFlux, Metric::class.java)

                }

                PUT("/metrics/{name}")
                {
                    it.bodyToMono(Metric::class.java)
                            .flatMap {
                                rSocketRequester.route("metrics/emit")
                                        .data(it)
                                        .send()
                            }
                            .flatMap {
                                ServerResponse.status(HttpStatus.CREATED).build()
                            }
                }
            }

}