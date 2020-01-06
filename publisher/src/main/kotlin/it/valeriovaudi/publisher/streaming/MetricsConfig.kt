package it.valeriovaudi.publisher.streaming

import org.springframework.amqp.core.*
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.DatabaseClient
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@Configuration
class MetricsConfig {


    @Bean
    fun queueNameGenerator() = AnonymousQueueNameGenerator(UUID.randomUUID().toString())

    @Bean
    fun jdbcMetricsRepository(databaseClient: DatabaseClient) =
            JdbcMetricsRepository(databaseClient)

    @Bean
    fun metricEmitter(metricsRepository: JdbcMetricsRepository,
                      rabbitTemplate: RabbitTemplate) =
            RabbitMQMetricsPublisher(ConcurrentLinkedQueue(), metricsRepository, rabbitTemplate)

    @Bean
    fun storeMetricsQueue(queueNameGenerator: AnonymousQueueNameGenerator): Queue =
            Queue(queueNameGenerator.anonymousQueueNameFor("storeMetricsQueue"), false, false, true)

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

class AnonymousQueueNameGenerator(private val seed: String) {

    fun anonymousQueueNameFor(name: String) = "$name.$seed.queue"

}