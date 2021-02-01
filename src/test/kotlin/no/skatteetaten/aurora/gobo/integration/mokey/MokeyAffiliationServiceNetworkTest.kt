package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.jsonResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import no.skatteetaten.aurora.gobo.service.AffiliationService

class MokeyAffiliationServiceNetworkTest {

    private val server = MockWebServer()
    private val affiliationService = AffiliationService(
        WebClient.create(server.url("/").toString()),
        WebClient.create(server.url("/").toString())
    )

    @Test
    fun `Get affiliations with retry`() {
        val failed = MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AFTER_REQUEST }
        val success = jsonResponse(listOf("aurora"))
        val requests = server.executeBlocking(failed, failed, success) {
            val affiliations = affiliationService.getAllDeployedAffiliations()
            assertThat(affiliations.first()).isEqualTo("aurora")
        }
        assertThat(requests).hasSize(3)
    }
}
