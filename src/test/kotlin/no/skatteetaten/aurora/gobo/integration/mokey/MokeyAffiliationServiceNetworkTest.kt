package no.skatteetaten.aurora.gobo.integration.mokey

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.service.AffiliationService
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.jsonResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class MokeyAffiliationServiceNetworkTest {

    private val server = MockWebServer()
    private val affiliationService = AffiliationService(
        WebClient.create(server.url("/").toString()),
        BooberWebClient(booberUrl = "/", webClient = WebClient.create(), objectMapper = ObjectMapper())
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
