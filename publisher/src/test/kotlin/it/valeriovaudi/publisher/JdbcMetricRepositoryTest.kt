package it.valeriovaudi.publisher

import io.r2dbc.spi.ConnectionFactories
import it.valeriovaudi.publisher.streaming.JdbcMetricsRepository
import it.valeriovaudi.publisher.streaming.Metric
import it.valeriovaudi.publisher.streaming.MetricsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.r2dbc.core.DatabaseClient
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import java.io.File

@Testcontainers
internal class JdbcMetricRepositoryTest {

    companion object {
        @JvmField
        @Container
        val container: DockerComposeContainer<*> = DockerComposeContainer<Nothing>(File("src/test/resources/docker-compose.yml"))
                .withExposedService("database_1", 3306)
    }

    lateinit var client: DatabaseClient
    lateinit var jdbcMetricsRepository: MetricsRepository

    @BeforeEach
    fun setUp() {
        val serviceHost = container.getServiceHost("database_1", 3306)
        val servicePort = container.getServicePort("database_1", 3306)

        val connectionFactory = ConnectionFactories.get("r2dbcs:mysql://root:root@$serviceHost:$servicePort/metrics")

        client = DatabaseClient.create(connectionFactory)
        jdbcMetricsRepository = JdbcMetricsRepository(client)
    }

    @Test
    fun emit() {
        val metrics = Metric(name = "A_METRIC_NAME", value = "A_VALUE")
        val emit = jdbcMetricsRepository.emit(metrics).toMono()

        StepVerifier.create(emit)
                .expectNext(metrics)
                .verifyComplete()
    }

    @Test
    fun sse() {
        val aMetric = Metric(name = "A_METRIC_NAME", value = "A_VALUE")
        val anotherMetric = Metric(name = "A_METRIC_NAME", value = "ANOTHER_VALUE")
        val anotherMetricAgain = Metric(name = "A_METRIC_NAME", value = "ANOTHER_VALUE_AGAIN")

        listOf(aMetric, anotherMetric, anotherMetricAgain)
                .map { metric ->
                    StepVerifier.create(jdbcMetricsRepository.emit(metric).toMono())
                            .expectNext(metric)
                            .verifyComplete()
                }

        StepVerifier.create(jdbcMetricsRepository.findAll("A_METRIC_NAME"))
                .expectNext(aMetric)
                .expectNext(anotherMetric)
                .expectNext(anotherMetricAgain)
                .verifyComplete()
    }
}