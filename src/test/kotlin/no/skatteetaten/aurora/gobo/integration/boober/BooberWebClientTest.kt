package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class BooberWebClientTest {
    private val server = MockWebServer()
    private val url = server.url("/").toString()
    private val client = BooberWebClient(url, WebClient.create(), testObjectMapper())

    @AfterEach
    fun tearDown() {
        kotlin.runCatching {
            server.shutdown()
        }
    }

    @Test
    fun `Get boober response`() {
        server.executeBlocking(Response(items = listOf("a", "b"))) {
            val response = client.get<String>(url = url, token = "test-token").responses()
            assertThat(response.size).isEqualTo(2)
        }
    }

    @Test
    fun `Failure in boober response`() {
        server.executeBlocking(Response(success = false, message = "failure", items = listOf("a", "b"))) {
            assertThat { client.get<String>(url, "test-token").response() }.isFailure().isInstanceOf(SourceSystemException::class)
        }
    }

    @Test
    fun `Exception in boober response`() {
        server.executeBlocking(400 to Response(success = false, message = "failure", items = listOf("a", "b"))) {
            assertThat { client.get<String>(url, "test-token") }.isFailure().isInstanceOf(WebClientResponseException::class)
        }
    }

    @Test
    fun `Get boober url with url template`() {
        val link = "/v2/auroraconfig/{auroraConfig}?reference={reference}"
        val booberUrl = client.getBooberUrl(link)
        assertThat(booberUrl).isEqualTo("$url$link")
    }
}
