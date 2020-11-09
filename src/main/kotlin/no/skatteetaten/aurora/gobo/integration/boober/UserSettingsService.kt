package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.graphql.usersettings.UserSettingsInput
import org.springframework.stereotype.Service

@Service
class UserSettingsService(private val booberWebClient: BooberWebClient) {

    suspend fun getUserSettings(token: String): UserSettingsResource =
        booberWebClient.get<UserSettingsResource>(
            url = "/v1/users/annotations/applicationDeploymentFilters",
            token = token
        ).responseOrNull() ?: UserSettingsResource()

    suspend fun updateUserSettings(token: String, userSettings: UserSettingsInput) {
        booberWebClient.patch<Unit>(
            url = "/v1/users/annotations/applicationDeploymentFilters",
            token = token,
            body = userSettings.applicationDeploymentFilters
        )
    }
}

data class UserSettingsResource(val applicationDeploymentFilters: List<ApplicationDeploymentFilterResource> = emptyList())
