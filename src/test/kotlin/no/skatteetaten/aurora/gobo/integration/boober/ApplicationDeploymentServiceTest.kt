package no.skatteetaten.aurora.gobo.integration.boober

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import assertk.assertions.prop
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.DeleteApplicationDeploymentInput
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.bodyAsString
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class ApplicationDeploymentServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    private val applicationDeploymentService =
        ApplicationDeploymentService(BooberWebClient(url.toString(), WebClient.create(), testObjectMapper()))
    private val input = DeleteApplicationDeploymentInput("namespace", "name")

    @Test
    fun `Delete application deployments success`() {
        val booberResponse = BooberDeleteResponse(
            success = true,
            reason = "abc",
            applicationRef = BooberApplicationRef("namespace", "name")
        )
        val request = server.executeBlocking(Response(booberResponse)) {
            val responses = applicationDeploymentService.deleteApplicationDeployment("token", input)
            assertThat(responses).hasSize(1)
            assertThat(responses.first().success).isTrue()
        }.first()!!
        assertThat(request.path).isEqualTo("/v1/applicationdeployment/delete")
        assertThat(request.bodyAsString()).isNotNull().contains("applicationRefs")
    }

    @Test
    fun `Delete application deployments error result`() {
        val response = Response(
            success = false,
            message = "failure",
            items = listOf(mapOf("abc" to "bcd"))
        )
        val requests = server.executeBlocking(response) {
            assertThat {
                applicationDeploymentService.deleteApplicationDeployment("token", input)
            }.isFailure().isInstanceOf(SourceSystemException::class).all {
                messageContains("failure")
                prop("errorMessage", SourceSystemException::errorMessage).isEqualTo("failure")
            }
        }
        assertThat(requests.first()?.path).isEqualTo("/v1/applicationdeployment/delete")
    }

    @Test
    fun `Get application deployment spec`() {
        val ref = ApplicationDeploymentRef("utv", "gobo")
        val response = Response(items = emptyList<String>())
        val request = server.executeBlocking(response) {
            applicationDeploymentService.getSpec("token", "auroraConfigName", "auroraConfigReference", listOf(ref))
        }.first()!!
        assertThat(request.path).contains("utv/gobo")
    }
}
