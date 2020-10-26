package no.skatteetaten.aurora.gobo.integration.skap

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.SkapJobForWebsealBuilder
import no.skatteetaten.aurora.gobo.integration.containsAuroraToken
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class RouteServiceTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "test-token"
    }
    private val jobService = RouteServiceReactive(
        sharedSecretReader, WebClient.create(server.url("/").toString())
    )

    @Test
    fun `get jobs`() {
        val job = SkapJobForWebsealBuilder().build()
        val request = server.executeBlocking(listOf(job, job), objectMapper = testObjectMapper()) {
            val jobs = jobService.getSkapJobs("dev", "app")
            assertThat(jobs.size).isEqualTo((2))
        }.first()

        assertThat(request).containsAuroraToken()
    }
}
