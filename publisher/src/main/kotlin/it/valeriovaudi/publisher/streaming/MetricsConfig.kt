package it.valeriovaudi.publisher.streaming

import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate
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
    fun metricEmitter(metricsRepository: JdbcMetricsRepository,
                      rabbitTemplate: RabbitTemplate) =
            MetricEmitter(ConcurrentLinkedQueue(), metricsRepository, rabbitTemplate)

    @Bean
    fun storeMetricsBus(): Queue =
            Queue("storeMetricsBus", false, false, false)

}