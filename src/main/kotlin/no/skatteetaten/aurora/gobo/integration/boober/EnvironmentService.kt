package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger { }

data class EnvironmentDeploymentRef(val environment: String, val application: String, val autoDeploy: Boolean)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MultiAffiliationEnvironment(
    val affiliation: String,
    val deploymentRefs: List<EnvironmentDeploymentRef>
) {
    fun getApplicationDeploymentRefs() = deploymentRefs.map { ApplicationDeploymentRef(it.environment, it.application) }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BooberEnvironmentResource(
    val affiliation: String,
    val applicationDeploymentRef: EnvironmentDeploymentRef?,
    val errorMessage: String?,
    val warningMessage: String?
) {
    fun logError() {
        logger.error { "Error from multi-affiliation: $errorMessage" }
    }

    fun logWarning() {
        if (!warningMessage.isNullOrEmpty()) {
            logger.warn { "Warning from multi-affiliation: application=${applicationDeploymentRef?.application} $warningMessage" }
        }
    }
}

@Service
class EnvironmentService(private val booberWebClient: BooberWebClient) {

    suspend fun getEnvironments(token: String, environment: String): List<MultiAffiliationEnvironment> =
        booberWebClient
            .get<BooberEnvironmentResource>(url = "/v2/search?environment=$environment", token = token)
            .responsesWithErrors()
            .let {
                it.logErrors()
                it.createMultiAffiliationEnvironments()
            }

    private fun ResponsesAndErrors<BooberEnvironmentResource>.logErrors() =
        errors.filterNot { it.errorMessage.isNullOrEmpty() }.forEach { it.logError() }

    private fun ResponsesAndErrors<BooberEnvironmentResource>.createMultiAffiliationEnvironments() =
        items.groupBy { it.affiliation }.map { resourcesByAffiliation ->
            MultiAffiliationEnvironment(
                affiliation = resourcesByAffiliation.key,
                deploymentRefs = resourcesByAffiliation.value.mapNotNull { resource ->
                    resource.applicationDeploymentRef.also { resource.logWarning() }
                }
            )
        }
}
