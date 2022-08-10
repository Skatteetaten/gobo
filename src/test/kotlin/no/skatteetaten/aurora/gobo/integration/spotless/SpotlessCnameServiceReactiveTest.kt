package no.skatteetaten.aurora.gobo.integration.spotless

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.CnameAzureBuilder
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

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
}
