package no.skatteetaten.aurora.gobo.integration.phil

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import no.skatteetaten.aurora.gobo.DeploymentResourceBuilder
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

class PhilServiceReactiveTest {

    private val server = MockWebServer()

    private val service = PhilServiceReactive(WebClient.create(server.url))

    @Test
    fun `Deploy environment`() {
        val request = server.executeBlocking(listOf(DeploymentResourceBuilder().build())) {
            val deployments = service.deployEnvironment("dev-utv", "test-token")
            assertThat(deployments).isNotNull().hasSize(1)
            assertThat(deployments?.first()?.deployId).isEqualTo("123")
        }.first()!!

        assertThat(request.path).contains("/environments")
        assertThat(request.headers[HttpHeaders.AUTHORIZATION]).isEqualTo("Bearer test-token")
    }

    @Test
    fun `Deploy environment with bad request response`() {
        server.executeBlocking(400 to "Bad request") {
            assertThat {
                service.deployEnvironment("dev-utv", "test-token")
            }.isFailure().isInstanceOf(PhilIntegrationException::class)
        }
    }
}
