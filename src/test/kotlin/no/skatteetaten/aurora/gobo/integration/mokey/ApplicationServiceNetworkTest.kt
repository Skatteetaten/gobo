package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Disabled("Network setup should be added for webclient")
class ApplicationServiceNetworkTest {
    private val server = MockWebServer()
    private val url = server.url("/").toString()

    private val applicationServiceBlocking = ApplicationServiceBlocking(ApplicationService(WebClient.create(url)))

    @Test
    fun `Retry on read timeout`() {
        val errorResponse = MockResponse().apply {
            socketPolicy = SocketPolicy.DISCONNECT_AFTER_REQUEST
        }
        val okResponse = MockResponse()
            .setBody(testObjectMapper().writeValueAsString(ApplicationResourceBuilder().build()))
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

        server.execute(errorResponse, okResponse) {
            val application = applicationServiceBlocking.getApplication("test123")
            assertThat(application).isNotNull()
        }
    }
}
