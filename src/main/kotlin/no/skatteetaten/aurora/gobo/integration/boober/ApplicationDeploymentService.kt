package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.JsonNode
import java.time.Duration
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

private val logger = KotlinLogging.logger { }

@Service
class ApplicationDeploymentService(private val booberWebClient: BooberWebClient) {

    fun deleteApplicationDeployment(
        token: String,
        input: DeleteApplicationDeploymentInput
    ): Boolean {
        val response = booberWebClient.post<JsonNode>(
            url = "/v1/applicationdeployment/delete",
            token = token,
            body = mapOf("applicationRefs" to listOf(input))
        ).toMono().blockNonNullWithTimeout()
        logger.debug { "Response from boober delete application deployment: $response" }

        return true
    }

    fun deploy(
        token: String,
        auroraConfig: String,
        reference: String,
        payload: ApplyPayload
    ): Response<DeployResource> {

        //TODO: Se på response format mtp validation errors og deployErrors
        val url = "/v1/apply/$auroraConfig?reference=$reference"
        return booberWebClient.executeMono<Response<DeployResource>>(token) {
            it.put().uri(booberWebClient.getBooberUrl(url), emptyMap<String, Any>())
                .body(BodyInserters.fromValue(payload))
        }.blockNonNullWithTimeout()
    }

    private fun <T> Mono<T>.blockNonNullWithTimeout() =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "boober")
}

data class DeleteApplicationDeploymentInput(val namespace: String, val name: String)

data class DeployResource(
    val auroraConfigRef: AuroraConfigRefResource,
    val deploymentSpec: JsonNode,
    val deployId: String,
    val openShiftResponses: List<JsonNode>,
    val success: Boolean = true,
    val reason: String? = null,
    val tagResponse: JsonNode? = null,
    val projectExist: Boolean = false,
    val warnings: List<String> = emptyList()
) {
    val cluster = deploymentSpec.at("/cluster/value").textValue()
    val environment = deploymentSpec.at("/envName/value").textValue()
    val name = deploymentSpec.at("/name/value").textValue()
    val version = deploymentSpec.at("/version/value").textValue()
    val releaseTo: String? = deploymentSpec.at("/releaseTo/value").textValue()
    val successString = if (success) "DEPLOYED" else "FAILED"
}

data class AuroraConfigRefResource(
    val name: String,
    val refName: String,
    val resolvedRef: String
)
