package no.skatteetaten.aurora.gobo.integration.spotless

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.CnameAzureBuilder
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.charset.Charset

class SpotlessCnameServiceReactiveTest {

    private val server = MockWebServer()
    private val service = SpotlessCnameServiceReactive(WebClient.create(server.url), "test")

    @Test
    fun `Get cname content`() {
        val requests = server.executeBlocking(listOf(CnameAzureBuilder().build())) {
            val cnameContent = service.getCnameContent(listOf("aurora"))
            assertThat(cnameContent).hasSize(1)
            assertThat(cnameContent.first().ownerObjectName).isEqualTo("demo")
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get cname content without affiliations`() {
        val requests = server.executeBlocking(
            listOf(
                CnameAzureBuilder("test").build(),
                CnameAzureBuilder("aurora-test").build()
            )
        ) {
            val cnameContent = service.getCnameContent(null)
            assertThat(cnameContent).hasSize(2)
            assertThat(cnameContent.first().ownerObjectName).isEqualTo("demo")
            assertThat(cnameContent.first().namespace).isNotEqualTo(cnameContent[1].namespace)
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get cname content 403 Forbidden`() {
        val e = WebClientResponseException.create(HttpStatus.FORBIDDEN.value(), "Forbidden from GET", HttpHeaders(), byteArrayOf(), Charset.defaultCharset())
        server.enqueue(
            MockResponse().setResponseCode(HttpStatus.FORBIDDEN.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("Forbidden")
        )
        runBlocking {
            assertThrows<WebClientResponseException> { service.getCnameContent(listOf("aurora")) }
        }
    }
}
