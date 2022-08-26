package no.skatteetaten.aurora.gobo.integration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import no.skatteetaten.aurora.webflux.AuroraRequestParser.KLIENTID_FIELD
import no.skatteetaten.aurora.webflux.AuroraRequestParser.KORRELASJONSID_FIELD
import no.skatteetaten.aurora.webflux.config.AuroraWebClientConfig
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.sleuth.autoconfig.zipkin2.ZipkinAutoConfiguration
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@SpringBootTest(
    classes = [
        AuroraWebClientConfig::class,
        WebClientAutoConfiguration::class,
        ZipkinAutoConfiguration::class,
        ApplicationConfig::class,
        SharedSecretReader::class
    ]
)
class DefaultHeadersTest {

    private val server = MockWebServer()

    @MockkBean
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    @TargetService(ServiceTypes.MOKEY)
    private lateinit var webClient: WebClient

    @Test
    fun `Verify Klientid and Korrelasjonsid headers`() {
        val request = server.execute(MockResponse()) {
            webClient.get().uri(server.url).retrieve().bodyToMono<Unit>().block()
        }

        val headers = request.first()?.headers!!
        assertThat(headers[KLIENTID_FIELD]).isEqualTo("gobo")
        assertThat(headers[USER_AGENT]).isEqualTo("gobo")
        assertThat(headers[KORRELASJONSID_FIELD]).isNotNull().isNotEmpty()
    }
}
