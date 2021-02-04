package no.skatteetaten.aurora.gobo.integration.naghub

import assertk.assertThat
import assertk.assertions.isSuccess
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class NagHubServiceTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "abc"
    }
    private val webClient = ApplicationConfig(500, 500, 500, "", sharedSecretReader)
        .webClientNagHub(server.url("/").toString(), WebClient.builder())
    private val nagHubService = NagHubService(webClient)

    @Test
    fun `Verify is able to send message`() {
        val response = jacksonObjectMapper().createObjectNode().toString()
        server.executeBlocking(response) {
            assertThat {
                nagHubService.sendMessage("12345", null, DetailedMessage(NagHubColor.Red, "hello"))
            }.isSuccess()
        }
    }
}
