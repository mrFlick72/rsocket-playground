package it.valeriovaudi.publisher.streaming

import org.reactivestreams.Publisher
import org.springframework.data.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono
import java.io.Serializable

data class Metric(val name: String, val value: String) : Serializable

interface MetricsRepository {

    fun emit(metric: Metric): Publisher<Metric>

    fun findAll(name: String): Publisher<Metric>

}

class JdbcMetricsRepository(private val databaseClient: DatabaseClient) : MetricsRepository {
    override fun emit(metric: Metric): Publisher<Metric> =
            databaseClient.execute("INSERT INTO METRICS (METRIC_NAME, METRIC_VALUE) VALUES (:name, :value)")
                    .bind("name", metric.name)
                    .bind("value", metric.value)
                    .fetch()
                    .rowsUpdated()
                    .flatMap { Mono.just(metric) }


    override fun findAll(name: String): Publisher<Metric> =
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