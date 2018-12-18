package no.skatteetaten.aurora.gobo.resolvers.usersettings

import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentFilterResource
import no.skatteetaten.aurora.gobo.integration.boober.UserSettingsResource

data class ApplicationDeploymentFilter(
    val name: String,
    val default: Boolean?,
    val affiliation: String,
    val applications: List<String> = emptyList(),
    val environments: List<String> = emptyList()
) {
    constructor(resource: ApplicationDeploymentFilterResource) : this(
        resource.name,
        resource.default,
        resource.affiliation,
        resource.applications,
        resource.environments

    )
}

data class UserSettings(val applicationDeploymentFilters: List<ApplicationDeploymentFilter> = emptyList()) {

    constructor(userSettingsResource: UserSettingsResource) : this(
        userSettingsResource.applicationDeploymentFilters.map { ApplicationDeploymentFilter(it) }
    )

    fun applicationDeploymentFilters(affiliations: List<String>? = null) =
        if (affiliations == null) {
            applicationDeploymentFilters
        } else {
            applicationDeploymentFilters.filter { affiliations.contains(it.affiliation) }
        }
}
