package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.graphql.auroraapimetadata.ClientConfig
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class AuroraApiMetadataServiceTest {

    private val server = MockWebServer()
    private val auroraApiMetadataService =
        AuroraApiMetadataService(BooberWebClient(server.url, WebClient.create(), jacksonObjectMapper()))

    @Test
    fun `Get client config`() {
        val response = Response(
            items = listOf(
                ClientConfig(
                    "guiUrlPattern",
                    "openshiftCluster",
                    "openshiftUrl",
                    1
                )
            )
        )
        val requests = server.executeBlocking(response) {
            val clientConfig = auroraApiMetadataService.getClientConfig()
            assertThat(clientConfig.gitUrlPattern).isEqualTo("guiUrlPattern")
        }
        assertThat(requests).hasSize(1)
    }

    @Test
    fun `Get config names`() {
        val response = Response(items = listOf("1", "2"))
        val requests = server.executeBlocking(response) {
            val configNames = auroraApiMetadataService.getConfigNames()
            assertThat(configNames.names).hasSize(2)
            assertThat(configNames.names[0]).isEqualTo("1")
        }
        assertThat(requests).hasSize(1)
    }
}
