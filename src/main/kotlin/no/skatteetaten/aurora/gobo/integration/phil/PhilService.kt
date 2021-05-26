package no.skatteetaten.aurora.gobo.integration.phil

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.Serializable
import java.util.Date
import kotlinx.coroutines.reactive.awaitFirstOrNull
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.RequiresPhil
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private val logger = KotlinLogging.logger {}

@Service
@ConditionalOnBean(RequiresPhil::class)
class PhilServiceReactive(
    @TargetService(ServiceTypes.PHIL) private val webClient: WebClient,
    val mapper: ObjectMapper
) : PhilService {
    override suspend fun deployEnvironment(environment: String, token: String): PhilResult {
        val response: List<Deployment>? =
            webClient
                .post()
                .uri("/environments/$environment")
                .header("Authorization", token)
                .retrieve()
                .bodyToMono<List<Deployment>>()
                .awaitFirstOrNull()
        return PhilResult(true, response)
    }
}

data class PhilResult(
    val success: Boolean,
    val deployments: List<Deployment>?
)

interface PhilService {
    suspend fun deployEnvironment(environment: String, token: String): PhilResult =
        integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Phil integration is disabled for this environment")
}

@Service
@ConditionalOnMissingBean(RequiresPhil::class)
class PhilServiceDisabled : PhilService

data class DeploymentRef(
    val cluster: String,
    val affiliation: String,
    val environment: String,
    val application: String
) :
    Serializable

data class Deployment(
    val deploymentRef: DeploymentRef,
    val deployId: String = "",
    val timestamp: Date,
    val message: String,
    val status: DeploymentStatus
)

enum class DeploymentStatus {
    SUCCESS,
    FAIL
}
