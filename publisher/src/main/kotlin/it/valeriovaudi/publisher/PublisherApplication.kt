package it.valeriovaudi.publisher

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@SpringBootApplication
class PublisherApplication

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
