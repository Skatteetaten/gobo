package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import org.springframework.stereotype.Service

data class EnvironmentDeploymentRef(val environment: String, val application: String, val autoDeploy: Boolean)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MultiAffiliationEnvironment(
    val affiliation: String,
    val deploymentRefs: List<EnvironmentDeploymentRef>
) {
    fun getApplicationDeploymentRefs() = deploymentRefs.map { ApplicationDeploymentRef(it.environment, it.application) }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MultiAffiliationResponse(
    val affiliation: String,
    val applicationDeploymentRef: EnvironmentDeploymentRef?,
    val errorMessage: String?,
    val warningMessage: String?
)

private val logger = KotlinLogging.logger { }

@Service
class EnvironmentService(private val booberWebClient: BooberWebClient) {

    suspend fun getEnvironments(token: String, environment: String): List<MultiAffiliationEnvironment> =
        booberWebClient
            .get<MultiAffiliationResponse>(url = "/v2/search?environment=$environment", token = token)
            .responsesWithErrors()
            .let { responsesAndErrors ->
                responsesAndErrors.errors.filterNot { it.errorMessage.isNullOrEmpty() }.forEach { response ->
                    logger.error { "Error from multi-affiliation: ${response.errorMessage}" }
                }

                responsesAndErrors.items.groupBy { it.affiliation }
                    .map {
                        val responses = it.value
                        MultiAffiliationEnvironment(
                            affiliation = it.key,
                            deploymentRefs = responses.mapNotNull { response ->
                                response.applicationDeploymentRef.also {
                                    if (!response.warningMessage.isNullOrEmpty()) {
                                        // TODO only log message once, can potentially create a lot of identical log statements
                                        logger.warn { "Warning from multi-affiliation: application=${it?.application} ${response.warningMessage}" }
                                    }
                                }
                            }
                        )
                    }
            }
}
