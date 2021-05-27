package no.skatteetaten.aurora.gobo.integration.phil

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Date
import kotlinx.coroutines.reactive.awaitFirstOrNull
import no.skatteetaten.aurora.gobo.RequiresPhil
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
@ConditionalOnBean(RequiresPhil::class)
class PhilServiceReactive(
    @TargetService(ServiceTypes.PHIL) private val webClient: WebClient,
    val mapper: ObjectMapper
) : PhilService {
    override suspend fun deployEnvironment(environment: String, token: String): List<DeploymentResource>? {
        return webClient
            .post()
            .uri("/environments/$environment")
            .header(HttpHeaders.AUTHORIZATION, token)
            .retrieve()
            .bodyToMono<List<DeploymentResource>>()
            .awaitFirstOrNull()
    }
}

interface PhilService {
    suspend fun deployEnvironment(environment: String, token: String): List<DeploymentResource>? =
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
