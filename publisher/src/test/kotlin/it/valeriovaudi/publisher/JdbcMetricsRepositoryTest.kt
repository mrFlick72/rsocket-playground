package it.valeriovaudi.publisher

import io.r2dbc.spi.ConnectionFactories
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
internal class JdbcMetricsRepositoryTest {

    companion object {
        @JvmField
        @Container
        val container: DockerComposeContainer<*> = DockerComposeContainer<Nothing>(File("src/test/resources/docker-compose.yml"))
                .withExposedService("database_1", 3306)
    }

    lateinit var client: DatabaseClient

    @BeforeEach
    fun setUp() {
        val serviceHost = container.getServiceHost("database_1", 3306)
        val servicePort = container.getServicePort("database_1", 3306)

        val connectionFactory = ConnectionFactories.get("r2dbcs:mysql://root:root@$serviceHost:$servicePort/metrics")

        client = DatabaseClient.create(connectionFactory)
    }

    @Test
    fun emit() {
        val jdbcMetricsRepository = JdbcMetricsRepository(client)

        val metrics = Metrics(name = "A_METRICS_NAME", value = "A_VALUE")
        val emit = jdbcMetricsRepository.emit(metrics).toMono()

        StepVerifier.create(emit)
                .expectNext(metrics)
                .verifyComplete()
    }

    @Test
    fun sse() {
    }
}