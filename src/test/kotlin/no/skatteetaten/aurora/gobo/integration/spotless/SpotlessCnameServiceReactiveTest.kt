package no.skatteetaten.aurora.gobo.integration.spotless

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
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

class SpotlessCnameServiceReactiveTest {

    private val server = MockWebServer()
    private val service = SpotlessCnameServiceReactive(WebClient.create(server.url), "test")

    @Test
    fun `Get cname content`() {
        val requests = server.executeBlocking(listOf(CnameAzureBuilder().build())) {
            val cnameContent = service.getCnameContent("aurora")
            assertThat(cnameContent).hasSize(1)
            assertThat(cnameContent.first().ownerObjectName).isEqualTo("demo-azure")
        }

        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get cname content 403 Forbidden`() {
        server.enqueue(
            MockResponse().setResponseCode(HttpStatus.FORBIDDEN.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("Forbidden")
        )
        runBlocking {
            assertThrows<WebClientResponseException> { service.getCnameContent("aurora") }
        }
    }
}
