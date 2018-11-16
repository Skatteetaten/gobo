package no.skatteetaten.aurora.gobo.resolvers.userSettings

import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentFilterResource
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsResource

data class ApplicationDeploymentFilter(
    val name: String,
    val affiliation: String,
    val applications: List<String> = emptyList(),
    val environments: List<String> = emptyList()
) {
    companion object {
        fun create(resource: ApplicationDeploymentFilterResource) =
            ApplicationDeploymentFilter(
                resource.name,
                resource.affiliation,
                resource.applications,
                resource.environments
            )
    }
}

data class UserSettings(val applicationDeploymentFilters: List<ApplicationDeploymentFilter> = emptyList()) {

    companion object {
        fun create(userSettingsResource: UserSettingsResource): UserSettings {
            val filters = userSettingsResource.applicationDeploymentFilters.map {
                ApplicationDeploymentFilter.create(it)
            }
            return UserSettings(filters)
        }
    }

    fun applicationDeploymentFilters(affiliations: List<String>? = null) =
        if (affiliations == null) {
            applicationDeploymentFilters
        } else {
            applicationDeploymentFilters.filter { affiliations.contains(it.affiliation) }
        }
}