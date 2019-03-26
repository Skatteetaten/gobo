package no.skatteetaten.aurora.gobo.integration

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.HEADER_KLIENTID
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.reactive.function.client.bodyToMono

@SpringTestTag
@SpringBootTest(classes = [ApplicationConfig::class])
class DefaultHeadersTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    @Autowired
    private lateinit var applicationConfig: ApplicationConfig

    @Test
    fun `Verify KlientID header`() {
        val webClient = applicationConfig.webClientBuilder(false).baseUrl(url.toString()).build()

        val request = server.execute(MockResponse()) {
            webClient.get().retrieve().bodyToMono<Unit>().block()
        }

        val headers = request.headers
        assertThat(headers[HEADER_KLIENTID]).isEqualTo("gobo")
    }
}