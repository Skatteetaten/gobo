package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
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
        val ref = ApplicationRef("namespace", "name")
        val requests = server.execute(Response(items = listOf(DeleteApplicationDeploymentResponse(ref, true, "")))) {
            val deleted = applicationDeploymentService.deleteApplicationDeployment("token", input)
            assertThat(deleted).isTrue()
        }
        assertThat(requests.first()?.path).isEqualTo("/v1/applicationdeployment/delete")
    }

    @Test
    fun `Delete application deployments error result`() {
        val response = Response(
            success = false,
            message = "failure",
            items = emptyList<DeleteApplicationDeploymentResponse>()
        )
        val requests = server.execute(response) {
            assertThat {
                applicationDeploymentService.deleteApplicationDeployment("token", input)
            }.isFailure().isInstanceOf(SourceSystemException::class).hasMessage("failure")
        }
        assertThat(requests.first()?.path).isEqualTo("/v1/applicationdeployment/delete")
    }
}