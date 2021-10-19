package no.skatteetaten.aurora.gobo.integration.phil

import kotlinx.coroutines.reactor.awaitFirst
import kotlinx.coroutines.reactor.awaitFirstOrNull
import no.skatteetaten.aurora.gobo.RequiresPhil
import no.skatteetaten.aurora.gobo.ServiceTypes.PHIL
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.integration.onStatusNotOk
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters.fromValue
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.Date

@Service
@ConditionalOnBean(RequiresPhil::class)
class EnvironmentServiceReactive(@TargetService(PHIL) private val webClient: WebClient) : EnvironmentService {
    override suspend fun getDeploymentStatus(
        deploymentRefs: List<DeploymentRefInput>,
        token: String
    ): List<DeploymentResource> =
        webClient
            .post()
            .uri("/deployments/status")
            .header(AUTHORIZATION, "Bearer $token")
            .body(fromValue(deploymentRefs))
            .retrieve()
            .onStatusNotOk { status, body ->
                throw PhilIntegrationException(
                    message = "Request failed when retreiving deployment statuses",
                    integrationResponse = body,
                    status = status
                )
            }
            .bodyToMono<List<DeploymentResource>>()
            .awaitFirst()

    override suspend fun deployEnvironment(environment: String, token: String) =
        webClient
            .post()
            .uri("/environments/{environment}", environment)
            .header(AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatusNotOk { status, body ->
                throw PhilIntegrationException(
                    message = "Request failed when deploying environment",
                    integrationResponse = body,
                    status = status
                )
            }
            .bodyToMono<List<DeploymentResource>>()
            .awaitFirstOrNull()

    override suspend fun deleteEnvironment(environment: String, token: String) =
        webClient
            .delete()
            .uri("/environments/{environment}", environment)
            .header(AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatusNotOk { status, body ->
                throw PhilIntegrationException(
                    message = "Request failed when deploying environment",
                    integrationResponse = body,
                    status = status
                )
            }
            .bodyToMono<List<DeletionResource>>()
            .awaitFirstOrNull()
}

interface EnvironmentService {
    suspend fun getDeploymentStatus(
        deploymentRefs: List<DeploymentRefInput>,
        token: String
    ): List<DeploymentResource> = integrationDisabled()

    suspend fun deployEnvironment(environment: String, token: String): List<DeploymentResource>? =
        integrationDisabled()

    suspend fun deleteEnvironment(environment: String, token: String): List<DeletionResource>? =
        integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Phil integration is disabled for this environment")
}

@Service
@ConditionalOnMissingBean(RequiresPhil::class)
class PhilDisabled : EnvironmentService

data class DeploymentRefResource(
    val cluster: String,
    val affiliation: String,
    val environment: String,
    val application: String
)

data class DeploymentRefInput(
    val affiliation: String,
    val environment: String,
    val application: String
)

data class DeploymentResource(
    val deploymentRef: DeploymentRefResource,
    val deployId: String? = null,
    val timestamp: Date,
    val message: String,
    val status: DeploymentStatus
)

enum class DeploymentStatus {
    REQUESTED,
    APPLIED,
    FAILED
}

data class DeletionResource(
    val deploymentRef: DeploymentRefResource,
    val timestamp: Date,
    val message: String?,
    val deleted: Boolean
)
