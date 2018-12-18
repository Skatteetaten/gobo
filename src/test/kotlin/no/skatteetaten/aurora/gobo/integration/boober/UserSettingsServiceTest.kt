package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assert
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.resolvers.usersettings.ApplicationDeploymentFilter
import no.skatteetaten.aurora.gobo.resolvers.usersettings.UserSettings
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient

class UserSettingsServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    private val applicationDeploymentFilterService =
        UserSettingsService(BooberWebClient(url.toString(), WebClient.create()))
    private val filter = ApplicationDeploymentFilterResource(
        "my filter",
        false,
        "aurora",
        listOf("app1", "app2"),
        listOf("env1", "env2")
    )
    private val response = Response(items = listOf(UserSettingsResource(listOf(filter))))

    @Test
    fun `Get application deployment filters`() {
        val request = server.execute(response) {
            val response = applicationDeploymentFilterService.getUserSettings("token")
            assert(response.applicationDeploymentFilters.size).isEqualTo(1)
            assert(response.applicationDeploymentFilters[0]).isEqualTo(filter)
        }

        assert(request.path).isEqualTo("/v1/users/annotations/applicationDeploymentFilters")
        assert(request.method).isEqualTo(HttpMethod.GET.name)
    }

    @Test
    fun `Get application deployment filters when no filters are present`() {
        val request = server.execute(Response(items = emptyList<UserSettingsResource>())) {
            val response = applicationDeploymentFilterService.getUserSettings("token")
            assert(response.applicationDeploymentFilters).isEmpty()
        }

        assert(request.path).isEqualTo("/v1/users/annotations/applicationDeploymentFilters")
        assert(request.method).isEqualTo(HttpMethod.GET.name)
    }

    @Test
    fun `Update user settings`() {
        val userSettings = UserSettings(listOf(ApplicationDeploymentFilter(filter)))
        val request = server.execute(response) {
            applicationDeploymentFilterService.updateUserSettings("token", userSettings)
        }

        assert(request.path).isEqualTo("/v1/users/annotations/applicationDeploymentFilters")
        assert(request.method).isEqualTo(HttpMethod.PATCH.name)
    }

    @Test
    fun `Remove application deployment filters`() {
        val userSettings = UserSettings(emptyList())
        val request = server.execute(Response(items = listOf(UserSettingsResource(emptyList())))) {
            applicationDeploymentFilterService.updateUserSettings("token", userSettings)
        }

        assert(request.path).isEqualTo("/v1/users/annotations/applicationDeploymentFilters")
        assert(request.method).isEqualTo(HttpMethod.PATCH.name)
    }
}