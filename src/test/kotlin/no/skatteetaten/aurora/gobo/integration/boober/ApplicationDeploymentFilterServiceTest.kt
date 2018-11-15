package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assert
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.integration.execute
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class ApplicationDeploymentFilterServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    private val applicationDeploymentFilterService =
        ApplicationDeploymentFilterService(BooberWebClient(url.toString(), WebClient.create()))

    @Test
    fun `Get application deployment filters`() {
        val filter =
            ApplicationDeploymentFilterResource("my filter", "aurora", listOf("app1", "app2"), listOf("env1", "env2"))
        val request = server.execute(Response(items = listOf(filter))) {
            val filters = applicationDeploymentFilterService.getFilters("token")
            assert(filters.size).isEqualTo(1)
            assert(filters[0]).isEqualTo(filter)
        }

        assert(request.path).isEqualTo("/v1/users/annotations/applicationDeploymentFilters")
    }
}