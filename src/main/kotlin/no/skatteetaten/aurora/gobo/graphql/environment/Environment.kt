package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadBatch
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

    fun status(dfe: DataFetchingEnvironment) =
        dfe.loadBatch<EnvironmentApplication, EnvironmentStatus>(this)
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
