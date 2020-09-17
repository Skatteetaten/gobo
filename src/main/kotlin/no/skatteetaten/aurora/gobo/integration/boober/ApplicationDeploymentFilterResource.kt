package no.skatteetaten.aurora.gobo.integration.boober

data class ApplicationDeploymentFilterResource(
    val name: String,
    val affiliation: String,
    val default: Boolean = false,
    val applications: List<String> = emptyList(),
    val environments: List<String> = emptyList()
)
