package it.valeriovaudi.publisher

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.r2dbc.core.DatabaseClient
import org.testcontainers.containers.DockerComposeContainer
import reactor.core.publisher.toMono
import reactor.test.StepVerifier
import java.io.File

@DataR2dbcTest
internal class JdbcMetricsRepositoryTest {

    /* companion object {
         @JvmField
         val container: DockerComposeContainer<*> = DockerComposeContainer<Nothing>(File("src/test/resources/docker-compose.yml"))
                 .withExposedService("postgres_1", 5432)
     }
 */
    @Autowired
    lateinit var client: DatabaseClient

    /*@BeforeEach
    fun setUp() {
        */
    /**
     * I prefer do not use docker port redirect in order to prevents the port conflicts on container start,
     * imaging it on a concurrent test suite, the code below is necessary in order to get the host and port
     * that the docker runtime assign to the container
     * *//*
        val serviceHost = container.getServiceHost("database_1", 3306)
        val servicePort = container.getServicePort("database_1", 3306)

        val connectionFactory = ConnectionFactories.get("r2dbcs:mysql://root:root@$serviceHost:$servicePort/metrics")

        create = DatabaseClient.create(connectionFactory)
    }*/

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