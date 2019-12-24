package it.valeriovaudi.consumer

import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.RSocketFactory.ClientRSocketFactory
import io.rsocket.frame.decoder.PayloadDecoder
import io.rsocket.transport.netty.client.TcpClientTransport
import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class ConsumerApplication {
    @Bean
    fun rSocket(): RSocket? {
        return RSocketFactory.connect()
            .mimeType(MimeTypeUtils.APPLICATION_JSON_VALUE, MimeTypeUtils.APPLICATION_JSON_VALUE)
            .frameDecoder(PayloadDecoder.ZERO_COPY)
            .transport(TcpClientTransport.create(7000))
            .start()
            .block()
    }


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

@RestController
class ConsumerMessages(private val rSocketRequester: RSocketRequester) {
    @GetMapping("/echo/{message}")
    fun run(@PathVariable message: String?): Publisher<String> {
        println(message)
        return rSocketRequester.route("echo").data(message!!).retrieveMono(String::class.java)
    }
}