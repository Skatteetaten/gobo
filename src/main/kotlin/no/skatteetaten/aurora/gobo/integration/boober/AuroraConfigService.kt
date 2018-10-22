package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.fge.jackson.jsonpointer.JsonPointer
import com.github.fge.jsonpatch.JsonPatch
import com.github.fge.jsonpatch.ReplaceOperation
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.stereotype.Service
import reactor.core.publisher.toMono

@Service
class AuroraConfigService(private val booberWebClient: BooberWebClient) {

    fun getApplicationFile(token: String, it: String): String {
        return booberWebClient
            .get<AuroraConfigFileResource>(token, it)
            .filter { it.type == AuroraConfigFileType.APP }
            .map { it.name }
            .toMono()
            .blockNonNullAndHandleError()
    }

    fun patch(
        token: String,
        version: String,
        auroraConfigFile: String,
        applicationFile: String
    ): AuroraConfigFileResource {
        return booberWebClient.patch<AuroraConfigFileResource>(
            token = token,
            url = auroraConfigFile.replace("{fileName}", applicationFile), // TODO placeholder cannot contain slash
            body = createVersionPatch(version)
        ).toMono()
            .blockNonNullAndHandleError()
    }

    private fun createVersionPatch(version: String): Map<String, String> {
        val jsonPatch = JsonPatch(listOf(ReplaceOperation(JsonPointer("/version"), TextNode(version))))
        return mapOf("content" to jacksonObjectMapper().writeValueAsString(jsonPatch))
    }

    fun redeploy(
        token: String,
        details: ApplicationDeploymentDetailsResource,
        applyLink: String
    ): JsonNode {
        val payload = ApplyPayload(listOf(details.applicationDeploymentCommand.applicationDeploymentRef))
        return booberWebClient.put<JsonNode>(token, applyLink, body = payload).toMono().blockNonNullAndHandleError()
    }
}