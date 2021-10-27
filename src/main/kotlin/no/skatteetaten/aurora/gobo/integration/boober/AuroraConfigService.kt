package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.fge.jackson.jsonpointer.JsonPointer
import com.github.fge.jsonpatch.AddOperation
import com.github.fge.jsonpatch.JsonPatch
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfig
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentRefResource
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

private val logger = KotlinLogging.logger { }

@Service
class AuroraConfigService(
    private val booberWebClient: BooberWebClient
) {

    suspend fun getAuroraConfig(token: String, auroraConfig: String, reference: String): AuroraConfig {
        return booberWebClient
            .get<AuroraConfig>(
                url = "/v2/auroraconfig/{auroraConfig}?reference={reference}",
                token = token,
                params = mapOf("auroraConfig" to auroraConfig, "reference" to reference)
            ).response()
    }

    suspend fun getAuroraConfigFiles(
        token: String,
        auroraConfigName: String,
        environment: String,
        application: String
    ): List<AuroraConfigFileResource> {
        return booberWebClient
            .get<AuroraConfigFileResource>(
                url = "/v1/auroraconfig/{auroraConfigName}/files/{environment}/{application}",
                token = token,
                params = mapOf(
                    "auroraConfigName" to auroraConfigName,
                    "environment" to environment,
                    "application" to application
                )
            ).responses()
    }

    suspend fun updateAuroraConfigFile(
        token: String,
        auroraConfig: String,
        reference: String,
        fileName: String,
        content: String,
        oldHash: String
    ): AuroraConfigFileResource {
        val url = "/v2/auroraconfig/{auroraConfig}?reference={reference}"
        val body = mapOf("content" to content, "fileName" to fileName)

        return booberWebClient.put<AuroraConfigFileResource>(
            url = url,
            params = mapOf("auroraConfig" to auroraConfig, "reference" to reference),
            body = body,
            token = token,
            etag = oldHash
        ).response()
    }

    suspend fun addAuroraConfigFile(
        token: String,
        auroraConfig: String,
        reference: String,
        fileName: String,
        content: String
    ): AuroraConfigFileResource {
        val url = "/v2/auroraconfig/{auroraConfig}?reference={reference}"
        val body = mapOf("content" to content, "fileName" to fileName)
        return booberWebClient.put<AuroraConfigFileResource>(
            url = url,
            params = mapOf("auroraConfig" to auroraConfig, "reference" to reference),
            body = body,
            token = token
        ).response()
    }

    suspend fun getApplicationFile(token: String, url: String): String {
        val auroraConfig = "(?<=/auroraconfig/)(.+)(?=(\\?))".toRegex().find(url)?.value
            ?: throw SourceSystemException(
                message = "Unable to lookup application file, could not find affiliation",
                integrationResponse = "Could not find auroraconfig in url from mokey, url:$url",
                sourceSystem = ServiceTypes.BOOBER
            )

        val queryParams = UriComponentsBuilder.fromHttpUrl(url).build().queryParams.toSingleValueMap()
        val queryParamsString = queryParams.keys.joinToString(separator = "") {
            "$it={$it}&"
        }.removeSuffix("&")

        val urlTemplate = "/v1/auroraconfig/{auroraConfig}?$queryParamsString"
        logger.debug { "getApplicationFile, url=$url urlTemplate=$urlTemplate" }

        return booberWebClient
            .get<AuroraConfigFileResource>(
                token = token,
                url = urlTemplate,
                params = queryParams.apply { put("auroraConfig", auroraConfig) }
            )
            .responses()
            .filter { it.type == AuroraConfigFileType.APP }
            .map { it.name }
            .first()
    }

    suspend fun patch(
        token: String,
        version: String,
        auroraConfigFile: String,
        applicationFile: String
    ): AuroraConfigFileResource {
        logger.debug { "patch, url=$auroraConfigFile applicationFile=$applicationFile" }

        return booberWebClient.patch<AuroraConfigFileResource>(
            token = token,
            url = auroraConfigFile.replace("{fileName}", applicationFile),
            body = createVersionPatch(version)
        ).response()
    }

    private fun createVersionPatch(version: String): Map<String, String> {
        val jsonPatch = JsonPatch(listOf(AddOperation(JsonPointer("/version"), TextNode(version))))
        return mapOf("content" to jacksonObjectMapper().writeValueAsString(jsonPatch))
    }

    suspend fun redeploy(
        token: String,
        details: ApplicationDeploymentDetailsResource,
        applyLink: String
    ): RedeployResponse {
        val payload = ApplyPayload(listOf(details.applicationDeploymentCommand.applicationDeploymentRef))
        val json = booberWebClient.put<JsonNode>(url = applyLink, token = token, body = payload).response()
        return RedeployResponse(json)
    }
}

data class RedeployResponse(private val json: JsonNode) {

    val applicationDeploymentId: String
        @JsonProperty("id")
        get() = json.at("/applicationDeploymentId").textValue()
            ?: throw IllegalStateException("No applicationDeploymentId found in response")

    val affiliation: String
        get() = json.at("/deploymentSpec/affiliation/value").textValue()
            ?: throw IllegalStateException("No affiliation found in response")
}

data class AuroraConfigFileResource(
    val name: String,
    val contents: String,
    val type: AuroraConfigFileType,
    val contentHash: String
)

enum class AuroraConfigFileType {
    DEFAULT,
    GLOBAL,
    GLOBAL_OVERRIDE,
    BASE,
    BASE_OVERRIDE,
    INCLUDE_ENV,
    ENV,
    ENV_OVERRIDE,
    APP,
    APP_OVERRIDE
}

data class ApplyPayload(
    val applicationDeploymentRefs: List<ApplicationDeploymentRefResource> = emptyList(),
    val overrides: Map<String, String> = mapOf()
)
