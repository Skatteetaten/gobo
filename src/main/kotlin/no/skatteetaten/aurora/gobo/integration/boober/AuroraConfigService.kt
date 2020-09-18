package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.fge.jackson.jsonpointer.JsonPointer
import com.github.fge.jsonpatch.AddOperation
import com.github.fge.jsonpatch.JsonPatch
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentRefResource
import no.skatteetaten.aurora.gobo.resolvers.auroraconfig.AuroraConfig
import no.skatteetaten.aurora.gobo.resolvers.auroraconfig.AuroraConfigFileResource
import org.springframework.stereotype.Service

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

    suspend fun updateAuroraConfigFile(
        token: String,
        auroraConfig: String,
        reference: String,
        fileName: String,
        content: String,
        oldHash: String
    ): Response<AuroraConfigFileResource> {
        val url = "/v2/auroraconfig/{auroraConfig}?reference={reference}"
        val body = mapOf("content" to content, "fileName" to fileName)

        return booberWebClient.put(url = url, params = mapOf("auroraConfig" to auroraConfig, "reference" to reference), body = body, token = token, etag = oldHash)
    }

    suspend fun addAuroraConfigFile(
        token: String,
        auroraConfig: String,
        reference: String,
        fileName: String,
        content: String
    ): Response<AuroraConfigFileResource> {
        val url = "/v2/auroraconfig/{auroraConfig}?reference={reference}"
        val body = mapOf("content" to content, "fileName" to fileName)
        return booberWebClient.put(url = url, params = mapOf("auroraConfig" to auroraConfig, "reference" to reference), body = body, token = token)
    }

    suspend fun getApplicationFile(token: String, it: String): String {
        return booberWebClient
            .get<AuroraConfigFileResource>(token = token, url = it)
            .response()
            .takeIf { it.type == AuroraConfigFileType.APP }
            ?.name!!
    }

    suspend fun patch(
        token: String,
        version: String,
        auroraConfigFile: String,
        applicationFile: String
    ): AuroraConfigFileResource {
        return booberWebClient.patch<AuroraConfigFileResource>(
            token = token,
            url = auroraConfigFile.replace("{fileName}", applicationFile), // TODO placeholder cannot contain slash
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
    ): JsonNode {
        val payload = ApplyPayload(listOf(details.applicationDeploymentCommand.applicationDeploymentRef))
        return booberWebClient.put<JsonNode>(url = applyLink, token = token, body = payload).response()
    }
}

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
