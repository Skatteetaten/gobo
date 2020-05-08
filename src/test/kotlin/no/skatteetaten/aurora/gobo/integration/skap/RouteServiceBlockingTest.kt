package no.skatteetaten.aurora.gobo.integration.skap

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.SkapJobBuilder
import no.skatteetaten.aurora.gobo.integration.containsAuroraToken
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class RouteServiceBlockingTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "test-token"
    }
    private val jobService = RouteServiceBlocking(
        RouteServiceReactive(sharedSecretReader, WebClient.create(server.url("/").toString()))
    )

    @Test
    fun `get progressions`() {
        val progression = SkapJobBuilder().build()
        val request = server.execute(listOf(progression, progression), objectMapper = testObjectMapper()) {
            val jobs = jobService.getProgressions("dev", "app")
            assertThat(jobs.size).isEqualTo((2))
        }.first()

        assertThat(request).containsAuroraToken()
    }
}
