package it.valeriovaudi.publisher

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.util.*

@SpringBootApplication
class PublisherApplication {

    @Bean
    fun instanceId() = UUID.randomUUID().toString()

}

fun main(args: Array<String>) {
    runApplication<PublisherApplication>(*args)
}