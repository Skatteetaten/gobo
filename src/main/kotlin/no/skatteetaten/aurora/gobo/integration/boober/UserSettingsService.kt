package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.resolvers.usersettings.UserSettings
import org.springframework.stereotype.Service

@Service
class UserSettingsService(private val booberWebClient: BooberWebClient) {

    suspend fun getUserSettings(token: String): UserSettingsResource =
        booberWebClient.get<UserSettingsResource>(
            url = "/v1/users/annotations/applicationDeploymentFilters",
            token = token
        ).responseOrNull() ?: UserSettingsResource()

    suspend fun updateUserSettings(token: String, userSettings: UserSettings) {
        booberWebClient.patch<Unit>(
            url = "/v1/users/annotations/applicationDeploymentFilters",
            token = token,
            body = userSettings.applicationDeploymentFilters
        )
    }
}

data class UserSettingsResource(val applicationDeploymentFilters: List<ApplicationDeploymentFilterResource> = emptyList())

data class ApplicationDeploymentFilterResource(
    val name: String,
    val affiliation: String,
    val default: Boolean = false,
    val applications: List<String> = emptyList(),
    val environments: List<String> = emptyList()
)
