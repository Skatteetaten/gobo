package no.skatteetaten.aurora.gobo.integration.boober

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.resolvers.usersettings.ApplicationDeploymentFilter
import no.skatteetaten.aurora.gobo.resolvers.usersettings.UserSettingsInput
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.executeBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient

class UserSettingsServiceTest {

    private val server = MockWebServer()
    private val url = server.url("/")

    private val applicationDeploymentFilterService =
        UserSettingsService(BooberWebClient(url.toString(), WebClient.create(), testObjectMapper()))
    private val filter = ApplicationDeploymentFilterResource(
        name = "my filter",
        affiliation = "aurora",
        applications = listOf("app1", "app2"),
        environments = listOf("env1", "env2")
    )
    private val response = Response(items = listOf(UserSettingsResource(listOf(filter))))

    @Test
    fun `Get application deployment filters`() {
        val request = server.executeBlocking(response) {
            val response = applicationDeploymentFilterService.getUserSettings("token")
            assertThat(response.applicationDeploymentFilters.size).isEqualTo(1)
            assertThat(response.applicationDeploymentFilters[0]).isEqualTo(filter)
        }.first()

        assertThat(request?.path).isEqualTo("/v1/users/annotations/applicationDeploymentFilters")
        assertThat(request?.method).isEqualTo(HttpMethod.GET.name)
    }

    @Test
    fun `Get application deployment filters when no filters are present`() {
        val request = server.executeBlocking(Response(items = emptyList<UserSettingsResource>())) {
            val response = applicationDeploymentFilterService.getUserSettings("token")
            assertThat(response.applicationDeploymentFilters).isEmpty()
        }.first()

        assertThat(request?.path).isEqualTo("/v1/users/annotations/applicationDeploymentFilters")
        assertThat(request?.method).isEqualTo(HttpMethod.GET.name)
    }

    @Test
    fun `Update user settings`() {
        val userSettings = UserSettingsInput(listOf(ApplicationDeploymentFilter(filter)))
        val request = server.executeBlocking(response) {
            applicationDeploymentFilterService.updateUserSettings("token", userSettings)
        }.first()

        assertThat(request?.path).isEqualTo("/v1/users/annotations/applicationDeploymentFilters")
        assertThat(request?.method).isEqualTo(HttpMethod.PATCH.name)
    }

    @Test
    fun `Remove application deployment filters`() {
        val userSettings = UserSettingsInput(emptyList())
        val request = server.executeBlocking(Response(items = listOf(UserSettingsResource(emptyList())))) {
            applicationDeploymentFilterService.updateUserSettings("token", userSettings)
        }.first()

        assertThat(request?.path).isEqualTo("/v1/users/annotations/applicationDeploymentFilters")
        assertThat(request?.method).isEqualTo(HttpMethod.PATCH.name)
    }
}
