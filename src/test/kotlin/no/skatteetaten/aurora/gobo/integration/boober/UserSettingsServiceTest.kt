package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assert
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.integration.execute
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class UserSettingsServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    private val applicationDeploymentFilterService =
        UserSettingsService(BooberWebClient(url.toString(), WebClient.create()))
    private val filter = ApplicationDeploymentFilterResource("my filter", "aurora", listOf("app1", "app2"), listOf("env1", "env2"))
    private val userSettings = UserSettingsResource(listOf(filter))

    @Test
    fun `Get application deployment filters`() {
        val request = server.execute(Response(items = listOf(UserSettingsResource(listOf(filter))))) {
            val response = applicationDeploymentFilterService.getUserSettings("token")
            assert(response.applicationDeploymentFilters.size).isEqualTo(1)
            assert(response.applicationDeploymentFilters[0]).isEqualTo(filter)
        }

        assert(request.path).isEqualTo("/v1/users/annotations/applicationDeploymentFilters")
    }
    @Test
    fun `update user settings`() {
        val filter = ApplicationDeploymentFilterResource("my filter", "aurora", listOf("app1", "app2"), listOf("env1", "env2"))
    }
}