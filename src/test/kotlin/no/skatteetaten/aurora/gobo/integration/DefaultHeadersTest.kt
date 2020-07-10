package no.skatteetaten.aurora.gobo.integration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.filter.logging.AuroraHeaderFilter.KORRELASJONS_ID
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.HEADER_KLIENTID
import no.skatteetaten.aurora.gobo.TestConfig
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@SpringBootTest(classes = [TestConfig::class, ApplicationConfig::class])
class DefaultHeadersTest {

    private val server = MockWebServer()

    @Autowired
    private lateinit var webClient: WebClient

    @Test
    fun `Verify KlientID and Korrelasjonsid headers`() {
        val request = server.execute(MockResponse()) {
            webClient.get().uri(server.url).retrieve().bodyToMono<Unit>().block()
        }

        val headers = request.first()?.headers!!
        assertThat(headers.get(HEADER_KLIENTID)).isEqualTo("gobo")
        assertThat(headers.get(KORRELASJONS_ID)).isNotNull().isNotEmpty()
    }

    @Test
    fun `Verify that Korrelasjonsid header value is forwarded in next request`() {
        val request = server.execute(MockResponse()) {
            webClient
                .get()
                .uri(server.url)
                .header(KORRELASJONS_ID, "abc123")
                .retrieve()
                .bodyToMono<Unit>().block()
        }

        val headers = request.first()?.headers!!
        assertThat(headers.get(KORRELASJONS_ID)).isEqualTo("abc123")
    }
}
