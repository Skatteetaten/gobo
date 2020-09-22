package no.skatteetaten.aurora.gobo.integration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.HEADER_KLIENTID
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.TestConfig
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import no.skatteetaten.aurora.webflux.AuroraRequestParser.KORRELASJONSID_FIELD
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@SpringBootTest(classes = [TestConfig::class, ApplicationConfig::class, SharedSecretReader::class])
class DefaultHeadersTest {

    private val server = MockWebServer()

    @Autowired
    @TargetService(ServiceTypes.MOKEY)
    private lateinit var webClient: WebClient

    @Test
    fun `Verify KlientID and Korrelasjonsid headers`() {
        val request = server.execute(MockResponse()) {
            webClient.get().uri(server.url).retrieve().bodyToMono<Unit>().block()
        }

        val headers = request.first()?.headers!!
        assertThat(headers.get(HEADER_KLIENTID)).isEqualTo("gobo")
        assertThat(headers.get(USER_AGENT)).isEqualTo("gobo")
        assertThat(headers.get(KORRELASJONSID_FIELD)).isNotNull().isNotEmpty()
    }

    @Test
    fun `Verify that Korrelasjonsid header value is forwarded in next request`() {
        val request = server.execute(MockResponse()) {
            webClient
                .get()
                .uri(server.url)
                .header(KORRELASJONSID_FIELD, "abc123")
                .retrieve()
                .bodyToMono<Unit>().block()
        }

        val headers = request.first()?.headers!!
        assertThat(headers.get(KORRELASJONSID_FIELD)).isEqualTo("abc123")
    }
}
