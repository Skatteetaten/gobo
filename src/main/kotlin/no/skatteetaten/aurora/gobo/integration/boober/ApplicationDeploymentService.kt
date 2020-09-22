package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.DeleteApplicationDeploymentInput
import no.skatteetaten.aurora.gobo.resolvers.auroraconfig.ApplicationDeploymentSpec
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

private val logger = KotlinLogging.logger { }

@Service
class ApplicationDeploymentService(private val booberWebClient: BooberWebClient) {

    suspend fun deleteApplicationDeployment(
        token: String,
        input: DeleteApplicationDeploymentInput
    ): Boolean {
        val response = booberWebClient.post<JsonNode>(
            url = "/v1/applicationdeployment/delete",
            token = token,
            body = mapOf("applicationRefs" to listOf(input))
        ).responses()
        logger.debug { "Response from boober delete application deployment: $response" }

        return true
    }

    suspend fun deploy(
        token: String,
        auroraConfig: String,
        reference: String,
        payload: ApplyPayload
    ): Response<DeployResource> {

        val url = "/v1/apply/{auroraConfig}?reference={reference}"
        return booberWebClient.put(url = url, params = mapOf("auroraConfig" to auroraConfig, "reference" to reference), body = payload)
    }

    // TODO this should support  a list of applicationSpecCommands that also takes in responseType and Defaults.
    // TODO Should we move the default/formatting code here instead of in boober?
    suspend fun getSpec(
        token: String,
        auroraConfigName: String,
        auroraConfigReference: String,
        applicationDeploymentReferenceList: List<ApplicationDeploymentRef>
    ): List<ApplicationDeploymentSpec> {

        val requestParam = applicationDeploymentReferenceList.joinToString(
            transform = { "adr=${it.environment}/${it.application}" },
            separator = "&"
        ) + "&reference={auroraConfigReference}"

        val url = "/v1/auroradeployspec/{auroraConfig}?$requestParam"
        return booberWebClient.get<JsonNode>(url = url, params = mapOf("auroraConfig" to auroraConfigName, "auroraConfigReference" to auroraConfigReference)).responses().map {
            ApplicationDeploymentSpec(it)
        }
    }

    private fun <T> Mono<T>.blockNonNullWithTimeout() =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "boober")
}

data class DeployResource(
    val auroraConfigRef: AuroraConfigRefResource,
    val applicationDeploymentId: String,
    val deploymentSpec: JsonNode,
    val deployId: String,
    val openShiftResponses: List<JsonNode>,
    val success: Boolean = true,
    val reason: String? = null,
    val tagResponse: JsonNode? = null,
    val projectExist: Boolean = false,
    val warnings: List<String> = emptyList()
) {
    val successString = if (success) "DEPLOYED" else "FAILED"
}

data class AuroraConfigRefResource(
    val name: String,
    val refName: String,
    val resolvedRef: String
)
