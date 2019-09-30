package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.bodyAsString
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

@MockWebServerTestTag
class ApplicationDeploymentServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    private val applicationDeploymentService =
        ApplicationDeploymentService(BooberWebClient(url.toString(), WebClient.create()))
    private val input = DeleteApplicationDeploymentInput("namespace", "name")

    @Test
    fun `Delete application deployments success`() {
        val requests = server.execute(Response(items = listOf("abc"))) {
            val deleted = applicationDeploymentService.deleteApplicationDeployment("token", input)
            assertThat(deleted).isTrue()
        }
        assertThat(requests.first()?.path).isEqualTo("/v1/applicationdeployment/delete")
        assertThat(requests.first()?.bodyAsString()).isNotNull().contains("applicationRefs")
    }

    @Test
    fun `Delete application deployments error result`() {
        val response = Response(
            success = false,
            message = "failure",
            items = listOf("abc")
        )
        val requests = server.execute(response) {
            assertThat {
                applicationDeploymentService.deleteApplicationDeployment("token", input)
            }.isFailure().isInstanceOf(SourceSystemException::class).hasMessage("failure")
        }
        assertThat(requests.first()?.path).isEqualTo("/v1/applicationdeployment/delete")
    }
}