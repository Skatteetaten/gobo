package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.annotations.GraphQLIgnore
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentDeploymentRef
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.StatusCheckResource

enum class EnvironmentStatusType {
    /**
     * No status from Phil or Mokey
     */
    INACTIVE,

    /**
     * Aurora configuration has been applied by Boober, but no ApplicationDeployment is registered.
     */
    APPLIED,

    /**
     * Failed in either Boober/Phil or Mokey for any application
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
        // TODO status from Phil must be added
        fun create(ad: ApplicationDeploymentResource?) = when {
            ad == null -> EnvironmentStatus(EnvironmentStatusType.INACTIVE)
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
    val autoDeploy: Boolean,
    val status: EnvironmentStatus
) {
    companion object {
        fun create(name: String, ad: ApplicationDeploymentResource?, deploymentRef: EnvironmentDeploymentRef?) =
            EnvironmentApplication(name, deploymentRef?.autoDeploy ?: false, EnvironmentStatus.create(ad))
    }
}

data class EnvironmentAffiliation(
    val name: String,
    @GraphQLIgnore
    val applications: List<EnvironmentApplication>
) {

    fun applications(autoDeployOnly: Boolean?) =
        if (autoDeployOnly == true) {
            applications.filter { it.autoDeploy }
        } else {
            applications
        }
}

data class Environment(val name: String, val affiliations: List<EnvironmentAffiliation>)

private fun List<StatusCheckResource>.detailedStatusMessage() =
    this.map { "${it.name}, ${it.description}" }
