package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class BooberWebClientTest {
    private val server = MockWebServer()
    private val url = server.url("/").toString()
    private val client = BooberWebClient(url, WebClient.create(), testObjectMapper())

    @Test
    fun `Get boober response`() {
        server.execute(Response(items = listOf("a", "b"))) {
            val response = client.get<String>("test-token", url)
            val result = response.collectList().block()!!
            assertThat(result.size).isEqualTo(2)
        }
    }

    @Test
    fun `Failure in boober response`() {
        server.execute(Response(success = false, message = "failure", items = listOf("a", "b"))) {
            val request = client.get<String>("test-token", url)
            assertThat { request.collectList().block() }.isFailure().isInstanceOf(SourceSystemException::class)
        }
    }

    @Test
    fun `Exception in boober response`() {
        server.execute(400 to Response(success = false, message = "failure", items = listOf("a", "b"))) {
            val request = client.get<String>("test-token", url)
            assertThat { request.collectList().block() }.isFailure()
                .hasMessage("Exception occurred in Boober integration.")
        }
    }
}
