package no.skatteetaten.aurora.gobo.resolvers.userSettings

data class ApplicationDeploymentFilter(
    val affiliation: String,
    val applicationNames: List<String> = emptyList(),
    val environmentNames: List<String> = emptyList()
)

data class UserSettings(val applicationDeploymentFilters: List<ApplicationDeploymentFilter> = emptyList()) {

    fun applicationDeploymentFilters(affiliation: String) : List<ApplicationDeploymentFilter> {
        return applicationDeploymentFilters.filter { it.affiliation == affiliation }
    }
}