package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadValue
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentDeploymentRef
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.StatusCheckResource
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentResource
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentStatus.APPLIED
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentStatus.FAILED
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentStatus.REQUESTED

enum class EnvironmentStatusType {
    /**
     * No status from Phil or Mokey
     */
    INACTIVE,

    /**
     * Aurora configuration has been requested from Phil by Boober, but is not yet applied.
     */
    REQUESTED,

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
    val details: String? = null,
    val applicationDeploymentId: String? = null
) {
    companion object {
        fun create(deployment: DeploymentResource) = when (deployment.status) {
            REQUESTED -> EnvironmentStatus(
                state = EnvironmentStatusType.REQUESTED,
                message = deployment.message,
            )
            APPLIED -> EnvironmentStatus(
                state = EnvironmentStatusType.APPLIED,
                message = deployment.message,
                details = deployment.deployId,
            )
            FAILED -> EnvironmentStatus(
                state = EnvironmentStatusType.FAILED,
                message = deployment.message,
                details = deployment.deployId,
            )
        }
        fun create(ad: ApplicationDeploymentResource) = when {
            ad.inactive() -> EnvironmentStatus(
                state = EnvironmentStatusType.INACTIVE,
                message = "Application is inactive..."
            )
            ad.failed() -> EnvironmentStatus(
                state = EnvironmentStatusType.FAILED,
                message = "Application failed deployment",
            )
            ad.success() -> EnvironmentStatus(
                state = EnvironmentStatusType.COMPLETED,
                applicationDeploymentId = ad.identifier,
            )
            ad.inProgress() -> EnvironmentStatus(
                state = EnvironmentStatusType.APPLIED,
                applicationDeploymentId = ad.identifier,
                message = "Application is deploying...",
            )
            else -> EnvironmentStatus(
                state = EnvironmentStatusType.FAILED,
                message = "Unexpected status during application deployment",
                details = "${ad.status.code} - ${ad.status.reasons.detailedStatusMessage()}"
            )
        }
    }
}

data class EnvironmentApplication(
    /**
     * Required to lookup status, but should not be exposed in the graphql schema
     */
    @GraphQLIgnore
    val affiliation: String,
    @GraphQLIgnore
    private val deploymentRef: EnvironmentDeploymentRef,
) {
    @GraphQLIgnore
    val environment = deploymentRef.environment

    val name = deploymentRef.application
    val autoDeploy = deploymentRef.autoDeploy

    fun status(dfe: DataFetchingEnvironment) = dfe.loadValue<EnvironmentApplication, EnvironmentStatus>(this)
}

data class EnvironmentAffiliation(
    val name: String,
    @GraphQLIgnore
    val applications: List<EnvironmentApplication>
) {

    fun applications(autoDeployOnly: Boolean? = null) =
        if (autoDeployOnly == true) {
            applications.filter { it.autoDeploy }
        } else {
            applications
        }
}

data class Environment(val name: String, val affiliations: List<EnvironmentAffiliation>)

private fun List<StatusCheckResource>.detailedStatusMessage() =
    this.map { "${it.name}, ${it.description}" }
