package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.resolvers.blockAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.usersettings.UserSettingsInput
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Duration

@Service
class UserSettingsService(private val booberWebClient: BooberWebClient) {

    fun getUserSettings(token: String): UserSettingsResource =
        booberWebClient.get<UserSettingsResource>(
            token,
            "/v1/users/annotations/applicationDeploymentFilters"
        ).toMono().blockWithTimeout() ?: UserSettingsResource()

    fun updateUserSettings(token: String, userSettings: UserSettingsInput) {
        booberWebClient.patch<Unit>(
            token,
            "/v1/users/annotations/applicationDeploymentFilters",
            body = userSettings.applicationDeploymentFilters
        ).toMono().blockNonNullWithTimeout()
    }

    private fun <T> Mono<T>.blockWithTimeout() = this.blockAndHandleError(Duration.ofSeconds(30), "boober")
    private fun <T> Mono<T>.blockNonNullWithTimeout() =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "boober")
}

data class UserSettingsResource(val applicationDeploymentFilters: List<ApplicationDeploymentFilterResource> = emptyList())
