package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.Response
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

    @Test
    fun `Delete application deployments`() {
        val ref = ApplicationRef("namespace", "name")
        val requests = server.execute(Response(items = listOf(DeleteApplicationDeploymentResponse(ref, true, "")))) {
            val deleted = applicationDeploymentService.deleteApplicationDeployments(
                "token",
                DeleteApplicationDeploymentsInput(listOf(ref))
            )
            assertThat(deleted.success).isTrue()
            assertThat(deleted.applicationRef.name).isEqualTo("name")
        }
        assertThat(requests.first()?.path).isEqualTo("/v1/applicationdeployment/delete")
    }
}