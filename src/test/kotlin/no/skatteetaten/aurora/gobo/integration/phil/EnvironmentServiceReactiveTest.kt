package no.skatteetaten.aurora.gobo.integration.phil

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.DeletionResourceBuilder
import no.skatteetaten.aurora.gobo.DeploymentResourceBuilder
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

class EnvironmentServiceReactiveTest {

    private val server = MockWebServer()

    private val service = EnvironmentServiceReactive(WebClient.create(server.url))

    @Test
    fun `Deploy environment`() {
        val request = server.executeBlocking(listOf(DeploymentResourceBuilder().build())) {
            val deployments = service.deployEnvironment("dev-utv", "test-token")
            assertThat(deployments).isNotNull().hasSize(1)
            assertThat(deployments?.first()?.deployId).isNull()
        }.first()!!

        assertThat(request.path).isNotNull().contains("/environments")
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

    @Test
    fun `Delete environment`() {
        val request = server.executeBlocking(listOf(DeletionResourceBuilder().build())) {
            val deletionResources = service.deleteEnvironment("dev-utv", "test-token")
            assertThat(deletionResources).isNotNull().hasSize(1)
            assertThat(deletionResources?.first()?.deleted)
            assertThat(deletionResources?.first()?.deploymentRef?.affiliation).isEqualTo("aurora")
            assertThat(deletionResources?.first()?.deploymentRef?.application).isEqualTo("gobo")
            assertThat(deletionResources?.first()?.deploymentRef?.environment).isEqualTo("dev-utv")
        }.first()!!

        assertThat(request.path).isNotNull().contains("/environments")
        assertThat(request.headers[HttpHeaders.AUTHORIZATION]).isEqualTo("Bearer test-token")
    }

    @Test
    fun `Delete environment with bad request response`() {
        server.executeBlocking(400 to "Bad request") {
            assertThat {
                service.deployEnvironment("dev-utv", "test-token")
            }.isFailure().isInstanceOf(PhilIntegrationException::class)
        }
    }
}
