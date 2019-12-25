package it.valeriovaudi.publisher

import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@SpringBootApplication
class PublisherApplication {

    @Bean
    fun jdbcMetricsRepository(databaseClient: DatabaseClient) = JdbcMetricsRepository(databaseClient)

}

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

@Controller
class RSocketMetricsEndPoint(private val metricsRepository: MetricsRepository) {

    @MessageMapping("metrics/sse")
    fun sse(name: String): Publisher<Metric> = Flux.interval(Duration.ofSeconds(1)).flatMap { metricsRepository.sse(name) }

    @MessageMapping("metrics/emit")
    fun emitter(metric: Metric): Publisher<Metric> = metricsRepository.emit(metric)
}

data class Metric(val name: String, val value: String)

interface MetricsRepository {

    fun emit(metric: Metric): Publisher<Metric>

    fun sse(name: String): Publisher<Metric>

}

class JdbcMetricsRepository(private val databaseClient: DatabaseClient) : MetricsRepository {
    override fun emit(metric: Metric): Publisher<Metric> =
            databaseClient.execute("INSERT INTO METRICS (METRIC_NAME, METRIC_VALUE) VALUES (:name, :value)")
                    .bind("name", metric.name)
                    .bind("value", metric.value)
                    .fetch()
                    .rowsUpdated()
                    .flatMap { Mono.just(metric) }


    override fun sse(name: String): Publisher<Metric> =
            databaseClient.execute("SELECT METRIC_NAME, METRIC_VALUE FROM METRICS WHERE METRIC_NAME=:name")
                    .bind("name", name)
                    .fetch()
                    .all()
                    .map { sqlRowMap ->
                        Metric(
                                sqlRowMap.getValue("METRIC_NAME") as String,
                                sqlRowMap.getValue("METRIC_VALUE") as String
                        )
                    }


}