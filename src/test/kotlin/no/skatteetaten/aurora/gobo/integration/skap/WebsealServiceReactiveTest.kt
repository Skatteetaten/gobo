package no.skatteetaten.aurora.gobo.integration.skap

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.WebsealStateResourceBuilder
import no.skatteetaten.aurora.gobo.integration.containsAuroraToken
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class WebsealServiceReactiveTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "test-token"
    }
    private val websealService =
        WebsealServiceReactive(sharedSecretReader, WebClient.create(server.url("/").toString()), "utv")

    @Test
    fun `Get WebSEAL state`() {
        val websealState = WebsealStateResourceBuilder().build()
        val request = server.executeBlocking(listOf(websealState, websealState)) {
            val states = websealService.getStates()
            assertThat(states.size).isEqualTo(2)
        }.first()

        assertThat(request).containsAuroraToken()
    }

    @Test
    fun `Get WebSEAL jobs`() {
        val websealState = WebsealStateResourceBuilder().build()
        val request = server.executeBlocking(listOf(websealState, websealState)) {
            val states = websealService.getStates()
            assertThat(states.size).isEqualTo(2)
        }.first()

        assertThat(request).containsAuroraToken()
    }
}
