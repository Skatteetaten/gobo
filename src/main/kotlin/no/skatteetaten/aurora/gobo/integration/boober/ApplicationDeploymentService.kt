package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.DeleteApplicationDeploymentInput
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.DeleteApplicationDeploymentsInput
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.ApplicationDeploymentSpec
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger { }

@Service
class ApplicationDeploymentService(private val booberWebClient: BooberWebClient) {

    suspend fun deleteApplicationDeployment(
        token: String,
        input: DeleteApplicationDeploymentInput
    ) {
        val response = booberWebClient.post<JsonNode>(
            url = "/v1/applicationdeployment/delete",
            token = token,
            body = mapOf("applicationRefs" to listOf(input))
        ).responses()
        logger.debug { "Response from boober delete application deployment: $response" }
    }

    suspend fun deleteApplicationDeployments(
        token: String,
        input: DeleteApplicationDeploymentsInput
    ) {
        val response = booberWebClient.post<BooberDeleteResponse>(
            url = "/v1/applicationdeployment/delete",
            token = token,
            body = mapOf("applicationRefs" to input.toDeleteApplicationDeploymentInputList(token))
        ).responses()
        logger.debug { "Response from boober delete application deployment: $response" }
    }

    private suspend fun DeleteApplicationDeploymentsInput.toDeleteApplicationDeploymentInputList(token: String):
        List<DeleteApplicationDeploymentInput> = booberWebClient.post<BooberExistsResponse>(
            url = "/v1/applicationdeployment/{auroraConfigName}",
            params = mapOf("auroraConfigName" to this.auroraConfigName),
            body = mapOf("adr" to this.applicationDeployments),
            token = token
        ).responses().map { DeleteApplicationDeploymentInput(it.applicationRef.namespace, it.applicationRef.name) }

    suspend fun deploy(
        token: String,
        auroraConfig: String,
        reference: String,
        payload: ApplyPayload
    ): Response<DeployResource> {
        val url = "/v1/apply/{auroraConfig}?reference={reference}"
        return booberWebClient
            .put(
                url = url,
                params = mapOf("auroraConfig" to auroraConfig, "reference" to reference),
                body = payload,
                token = token
            )
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
        return booberWebClient.get<JsonNode>(
            url = url,
            params = mapOf("auroraConfig" to auroraConfigName, "auroraConfigReference" to auroraConfigReference),
            token = token
        ).responses().map {
            ApplicationDeploymentSpec(it)
        }
    }
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

data class BooberExistsResponse(
    val applicationRef: BooberApplicationRef,
    val exists: Boolean,
    val success: Boolean,
    val message: String
)

data class BooberDeleteResponse(
    val applicationRef: BooberApplicationRef,
    val success: Boolean,
    val reason: String
)

data class BooberApplicationRef(val namespace: String, val name: String)
