package it.valeriovaudi.publisher

import kotlinx.coroutines.flow.flatMap
import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.data.r2dbc.core.flow
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono

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

data class Metrics(val name: String, val value: String)

interface MetricsRepository {

    fun emit(metrics: Metrics): Publisher<Metrics>

    fun sse(name: String): Publisher<Metrics>

}

class JdbcMetricsRepository(private val databaseClient: DatabaseClient) : MetricsRepository {
    override fun emit(metrics: Metrics): Publisher<Metrics> =
            databaseClient.execute("INSERT INTO METRICS (METRICS_NAME, METRICS_VALUE) VALUES (:name, :value)")
                    .bind("name", metrics.name)
                    .bind("value", metrics.value)
                    .fetch()
                    .rowsUpdated()
                    .flatMap { Mono.just(metrics) }


    override fun sse(name: String): Publisher<Metrics> =
            databaseClient.execute("SELECT METRICS_NAME, METRICS_VALUE FROM METRICS WHERE METRICS_NAME=:name")
                    .bind("name", name)
                    .fetch()
                    .all()
                    .map { sqlRowMap ->
                        Metrics(
                                sqlRowMap.getValue("METRICS_NAME") as String,
                                sqlRowMap.getValue("METRICS_VALUE") as String
                        )
                    }


}