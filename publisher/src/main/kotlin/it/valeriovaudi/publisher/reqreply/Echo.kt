package it.valeriovaudi.publisher.reqreply

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class RSocketMessagingEndPoint {

    @MessageMapping("echo")
    fun echo(message: String): String {
        println(message)
        return "echo of message: $message"
    }

}