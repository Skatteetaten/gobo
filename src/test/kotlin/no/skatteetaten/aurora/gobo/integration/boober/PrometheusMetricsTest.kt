package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import com.expediagroup.graphql.server.spring.GraphQLAutoConfiguration
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfig
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import no.skatteetaten.aurora.springboot.AuroraSpringSecurityConfig
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@AutoConfigureMetrics
@EnableAutoConfiguration(
    exclude = [
        GraphQLAutoConfiguration::class,
        AuroraSpringSecurityConfig::class,
        ReactiveSecurityAutoConfiguration::class,
        ReactiveManagementWebSecurityAutoConfiguration::class
    ]
)
@SpringBootTest(
    classes = [ PrometheusMetricsTest.TestConfig::class, AuroraConfigService::class, BooberWebClient::class ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["boober.metrics.enabled=true"]
)
class PrometheusMetricsTest {

    @Autowired
    private lateinit var auroraConfigService: AuroraConfigService

    companion object {
        val server = MockWebServer()
        var port: String = SocketUtils.findAvailableTcpPort().toString()

        @JvmStatic
        @DynamicPropertySource
        fun testProperties(registry: DynamicPropertyRegistry) {
            registry.apply {
                add("integrations.boober.url", server::url)
                add("management.server.port", port::toString)
            }
        }
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        @TargetService(ServiceTypes.BOOBER)
        fun webClient(builder: WebClient.Builder) = builder.build()

        @Bean
        fun objectMapper() = testObjectMapper()
    }

    @Test
    fun `WebClient metrics from prometheus`() {
        val response1 = Response(AuroraConfig("aurora", "master", "master", emptyList()))
        val response2 = Response(AuroraConfigFileResource("name", "", AuroraConfigFileType.APP, ""))
        server.executeBlocking(response1, response2) {
            auroraConfigService.getAuroraConfig(token = "token", auroraConfig = "aurora", reference = "master")
            auroraConfigService.patch(token = "token", version = "1", auroraConfigFile = "/v1/{fileName}", applicationFile = "app")
        }

        val result = WebClient.create("http://localhost:$port")
            .get()
            .uri("/actuator/prometheus")
            .retrieve()
            .bodyToMono<String>()
            .block()

        assertThat(result).isNotNull().contains("/v2/auroraconfig/{auroraConfig}?reference={reference}")
        assertThat(result).isNotNull().contains("/v1/app")
    }
}
