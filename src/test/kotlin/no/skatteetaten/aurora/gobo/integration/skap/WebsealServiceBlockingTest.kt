package no.skatteetaten.aurora.gobo.integration.skap

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.WebsealStateBuilder
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.containsAuroraToken
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

@MockWebServerTestTag
class WebsealServiceBlockingTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "test-token"
    }
    private val websealService = WebsealServiceBlocking(
        WebsealService(sharedSecretReader, WebClient.create(server.url("/").toString()))
    )

    @Test
    fun `Get WebSEAL state`() {
        val websealState = WebsealStateBuilder().build()
        val request = server.execute(listOf(websealState, websealState)) {
            val states = websealService.getStates()
            assertThat(states.size).isEqualTo(2)
        }.first()

        assertThat(request).containsAuroraToken()
    }
}