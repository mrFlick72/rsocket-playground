package it.valeriovaudi.publisher.streaming

import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.DatabaseClient
import java.util.concurrent.ConcurrentLinkedQueue

@Configuration
class SSEUseCaseConfig {
    @Bean
    fun jdbcMetricsRepository(databaseClient: DatabaseClient) =
            JdbcMetricsRepository(databaseClient)

    @Bean
    fun metricEmitter() =
            MetricEmitter(ConcurrentLinkedQueue())

    @Bean
    fun storeMetricsBus(): Queue =
            Queue("storeMetricsBus", false, false, false)

    @Bean
    fun metricsEmitterListener(metricEmitter: MetricEmitter): MetricsEmitterListener =
            MetricsEmitterListener(metricEmitter)
}