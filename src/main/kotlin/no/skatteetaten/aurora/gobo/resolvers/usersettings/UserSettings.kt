package no.skatteetaten.aurora.gobo.resolvers.usersettings

import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentFilterResource
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsResource

data class UserSettings(val applicationDeploymentFilters: List<ApplicationDeploymentFilterInput> = emptyList()) {
    constructor(userSettingsResource: UserSettingsResource) : this(
            userSettingsResource.applicationDeploymentFilters.map { ApplicationDeploymentFilterInput(it) }
    )

    fun applicationDeploymentFilters(affiliations: List<String>? = null) =
            if (affiliations == null) {
                applicationDeploymentFilters
            } else {
                applicationDeploymentFilters.filter { affiliations.contains(it.affiliation) }
            }
}

data class ApplicationDeploymentFilterInput(
        val name: String,
        val affiliation: String,
        val default: Boolean = false,
        val applications: List<String> = emptyList(),
        val environments: List<String> = emptyList()
) {
    constructor(resource: ApplicationDeploymentFilterResource) : this(
            resource.name,
            resource.affiliation,
            resource.default,
            resource.applications,
            resource.environments
    )

    companion object {
        fun defaultApplicationDeploymentFilter(name: String, affiliation: String) =
                ApplicationDeploymentFilterInput(name, affiliation, true)
    }
}
