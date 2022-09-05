package no.skatteetaten.aurora.gobo.integration.naghub

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.ApplicationConfig
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class NagHubServiceReactiveTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "abc"
    }
    private val webClient = ApplicationConfig(500, 1500, 300000, sharedSecretReader)
        .webClientNagHub(server.url("/").toString(), WebClient.builder())
    private val nagHubService = NagHubServiceReactive(webClient)

    @Test
    fun `Verify is able to send message`() {
        val response = jacksonObjectMapper().createObjectNode().toString()
        val request = server.executeBlocking(response) {
            assertThat {
                nagHubService.sendMessage(
                    channelId = "12345",
                    simpleMessage = "hello world",
                    messages = listOf(
                        DetailedMessage(NagHubColor.Red, "hello")
                    )
                )
            }.isSuccess().given {
                assertThat(it?.success).isNotNull().isTrue()
            }
        }

        assertThat(request.first()?.path).isEqualTo("/api/v1/notify/12345")
    }

    @Test
    fun `Should return false when receives error from naghub`() {
        val response = jacksonObjectMapper().createObjectNode().toString()
        server.executeBlocking(500 to response) {
            assertThat {
                nagHubService.sendMessage(
                    channelId = "12345",
                    simpleMessage = "hello world",
                    messages = listOf(
                        DetailedMessage(NagHubColor.Red, "hello"),
                        DetailedMessage(NagHubColor.Red, "hello2")
                    )
                )
            }.isSuccess()
                .given {
                    assertThat(it?.success).isNotNull().isFalse()
                }
        }
    }
}
