package no.skatteetaten.aurora.gobo.integration.phil

import java.util.Date
import kotlinx.coroutines.reactive.awaitFirstOrNull
import no.skatteetaten.aurora.gobo.RequiresPhil
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.integration.onStatusNotOk
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
@ConditionalOnBean(RequiresPhil::class)
class PhilServiceReactive(
    @TargetService(ServiceTypes.PHIL) private val webClient: WebClient
) : PhilService {
    override suspend fun deployEnvironment(environment: String, token: String) =
        webClient
            .post()
            .uri("/environments/$environment")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
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
            .uri("/environments/$environment")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
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

interface PhilService {
    suspend fun deployEnvironment(environment: String, token: String): List<DeploymentResource>? =
        integrationDisabled()

    suspend fun deleteEnvironment(environment: String, token: String): List<DeletionResource>? =
        integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Phil integration is disabled for this environment")
}

@Service
@ConditionalOnMissingBean(RequiresPhil::class)
class PhilServiceDisabled : PhilService

data class DeploymentRefResource(
    val cluster: String,
    val affiliation: String,
    val environment: String,
    val application: String
)

data class DeploymentResource(
    val deploymentRef: DeploymentRefResource,
    val deployId: String = "",
    val timestamp: Date,
    val message: String,
    val status: DeploymentStatus
)

enum class DeploymentStatus {
    SUCCESS,
    FAIL
}

data class DeletionResource(
    val deploymentRef: DeploymentRefResource,
    val timestamp: Date,
    val message: String?,
    val deleted: Boolean
)
