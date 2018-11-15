package no.skatteetaten.aurora.gobo.resolvers.userSettings

data class ApplicationDeploymentFilter(
    val name: String,
    val affiliation: String,
    val applications: List<String> = emptyList(),
    val environments: List<String> = emptyList()
)

data class UserSettings(val applicationDeploymentFilters: List<ApplicationDeploymentFilter> = emptyList()) {

    fun applicationDeploymentFilters(affiliations: List<String>? = null) =
        if (affiliations == null) {
            applicationDeploymentFilters
        } else {
            applicationDeploymentFilters.filter { affiliations.contains(it.affiliation) }
        }
}