package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.MultiAffiliationResponseBuilder
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class EnvironmentServiceTest {

    private val server = MockWebServer()
    private val service = EnvironmentService(BooberWebClient(server.url, WebClient.create(), jacksonObjectMapper()))

    @Test
    fun `Get applicationDeploymentRefs for environment`() {
        val request = server.executeBlocking(Response(MultiAffiliationResponseBuilder().build())) {
            val envs = service.getEnvironments("test-token", "utv")
            val ref = envs.first().deploymentRefs.first()

            assertThat(envs.first().affiliation).isEqualTo("aurora")
            assertThat(ref.application).isEqualTo("gobo")
            assertThat(ref.environment).isEqualTo("utv")
        }.first()!!

        assertThat(request.path).isNotNull().contains("utv")
    }

    @Test
    fun `Get partial result for environment`() {
        val response = Response(
            success = true,
            items = listOf(MultiAffiliationResponseBuilder().build()),
            errors = listOf(MultiAffiliationResponseBuilder(errorMessage = "Something failed").build())
        )
        server.executeBlocking(response) {
            val envs = service.getEnvironments("test-token", "utv")
            assertThat(envs).hasSize(1)
            assertThat(envs.first().affiliation).isEqualTo("aurora")
        }
    }
}
