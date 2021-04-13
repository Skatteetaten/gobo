package no.skatteetaten.aurora.gobo.graphql.environment

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.StatusCheckResource

enum class EnvironmentStatusType {
    /**
     * Aurora configuration has been applied by Boober, but no ApplicationDeployment is registered.
     */
    APPLIED,

    /**
     * Failed in either Boober/Phil or Mokey
     */
    FAILED,

    /**
     * The deployment of the environment is completed
     */
    COMPLETED
}

data class EnvironmentStatus(
    val state: EnvironmentStatusType,
    val message: String? = null,
    val details: String? = null
) {
    companion object {
        fun create(ad: ApplicationDeploymentResource) = when {
            ad.success() -> EnvironmentStatus(EnvironmentStatusType.COMPLETED)
            else -> EnvironmentStatus(
                EnvironmentStatusType.FAILED,
                "Application not deployed",
                "${ad.status.code} - ${ad.status.reasons.detailedStatusMessage()}"
            )
        }
    }
}

data class EnvironmentApplication(
    val name: String,
    val status: EnvironmentStatus
) {
    companion object {
        fun create(ad: ApplicationDeploymentResource) = EnvironmentApplication(ad.name, EnvironmentStatus.create(ad))
    }
}

data class EnvironmentAffiliation(val name: String, val applications: List<EnvironmentApplication>)
data class Environment(val name: String, val affiliations: List<EnvironmentAffiliation>)

private fun List<StatusCheckResource>.detailedStatusMessage() =
    this.map { "${it.name}, ${it.description}" }
