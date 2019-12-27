package it.valeriovaudi.publisher.streaming

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.DatabaseClient
import java.util.concurrent.ConcurrentLinkedQueue

@Configuration
class MetricsConfig {
    @Bean
    fun jdbcMetricsRepository(databaseClient: DatabaseClient) =
            JdbcMetricsRepository(databaseClient)

    @Bean
    fun metricEmitter(metricsRepository: JdbcMetricsRepository,
                      rabbitTemplate: RabbitTemplate) =
            RabbitMQMetricsPublisher(ConcurrentLinkedQueue(), metricsRepository, rabbitTemplate)

    @Bean
    fun storeMetricsQueue(): Queue =
            Queue("storeMetricsQueue", false, false, false)

    @Bean
    fun storeMetricsExchange(): Exchange =
            DirectExchange("storeMetricsExchange", false, false)

    @Bean
    fun storeMetricsBinder(storeMetricsQueue: Queue,
                           storeMetricsExchange: Exchange) =
            Declarables(storeMetricsQueue, storeMetricsExchange,
                    BindingBuilder
                            .bind(storeMetricsQueue)
                            .to(storeMetricsExchange)
                            .with("store-metrics")
                            .noargs()
            )

}