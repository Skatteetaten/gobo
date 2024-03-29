package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger { }

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentDeploymentRef(val environment: String, val application: String, val autoDeploy: Boolean)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentResource(
    val affiliation: String,
    val deploymentRefs: List<EnvironmentDeploymentRef>
) {
    fun containsEnvironment(envName: String) = deploymentRefs.any { it.environment == envName }

    fun deploymentRefs(envName: String) = deploymentRefs.filter { it.environment == envName }

    fun getApplicationDeploymentRefs(envName: String) = deploymentRefs
        .map { ApplicationDeploymentRef(it.environment, it.application) }
        .filter { it.environment == envName }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class BooberEnvironmentDeploymentRef(val environment: String, val application: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BooberEnvironmentResource(
    val affiliation: String,
    val autoDeploy: Boolean,
    val applicationDeploymentRef: BooberEnvironmentDeploymentRef?,
    val errorMessage: String?,
    val warningMessage: String?
) {
    fun logError() {
        errorMessage?.let {
            logger.error { "Error from multi-affiliation: $it" }
        }
    }

    fun logWarning() {
        warningMessage?.let {
            logger.warn { "Warning from multi-affiliation: application=${applicationDeploymentRef?.application} $it" }
        }
    }
}

@Service
class EnvironmentService(private val booberWebClient: BooberWebClient) {

    suspend fun getEnvironments(token: String, environment: String): List<EnvironmentResource> =
        booberWebClient
            .get<BooberEnvironmentResource>(url = "/v2/search?environment=$environment", token = token)
            .responsesWithErrors()
            .let {
                it.logErrors()
                it.buildMultiAffiliationEnvironments()
            }

    private fun ResponsesAndErrors<BooberEnvironmentResource>.logErrors() =
        errors.forEach { it.logError() }

    private fun ResponsesAndErrors<BooberEnvironmentResource>.buildMultiAffiliationEnvironments() =
        items.groupBy { it.affiliation }.map { resourcesByAffiliation ->
            EnvironmentResource(
                affiliation = resourcesByAffiliation.key,
                deploymentRefs = resourcesByAffiliation.value.mapNotNull { resource ->
                    resource.logWarning()
                    resource.applicationDeploymentRef?.let {
                        EnvironmentDeploymentRef(it.environment, it.application, resource.autoDeploy)
                    }
                }
            )
        }
}
