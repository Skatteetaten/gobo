package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.fge.jackson.jsonpointer.JsonPointer
import com.github.fge.jsonpatch.AddOperation
import com.github.fge.jsonpatch.JsonPatch
import java.time.Duration
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentRefResource
import no.skatteetaten.aurora.gobo.resolvers.auroraconfig.AuroraConfig
import no.skatteetaten.aurora.gobo.resolvers.auroraconfig.ChangedAuroraConfigFileResponse
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Service
class AuroraConfigService(
    private val booberWebClient: BooberWebClient
) {

    fun getAuroraConfig(token: String, auroraConfig: String, reference: String): AuroraConfig {
        return booberWebClient
            .get<AuroraConfig>(token, "/v2/auroraconfig/$auroraConfig?reference=$reference")
            .toMono()
            .blockNonNullWithTimeout()
    }

    fun updateAuroraConfigFile(
        token: String,
        auroraConfig: String,
        reference: String,
        fileName: String,
        content: String,
        oldHash: String
    ): Response<AuroraConfigFileResource> {
        val url = "/v2/auroraconfig/$auroraConfig?reference=$reference"
        val body = mapOf("content" to content, "fileName" to fileName)

        return booberWebClient.executeMono<Response<AuroraConfigFileResource>>(token, etag = oldHash) {
            it.put().uri(booberWebClient.getBooberUrl(url), emptyMap<String, Any>()).body(BodyInserters.fromValue(body))
        }.blockNonNullWithTimeout()
    }

    fun addAuroraConfigFile(
        token: String,
        auroraConfig: String,
        reference: String,
        fileName: String,
        content: String
    ): Response<AuroraConfigFileResource> {
        val url = "/v2/auroraconfig/$auroraConfig?reference=$reference"
        val body = mapOf("content" to content, "fileName" to fileName)

        return booberWebClient.executeMono<Response<AuroraConfigFileResource>>(token) {
            it.put().uri(booberWebClient.getBooberUrl(url), emptyMap<String, Any>()).body(BodyInserters.fromValue(body))
        }.blockNonNullWithTimeout()
    }

    fun getApplicationFile(token: String, it: String): String {
        return booberWebClient
            .get<AuroraConfigFileResource>(token, it)
            .filter { it.type == AuroraConfigFileType.APP }
            .map { it.name }
            .toMono()
            .blockNonNullWithTimeout()
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
            .blockNonNullWithTimeout()
    }

    private fun createVersionPatch(version: String): Map<String, String> {
        val jsonPatch = JsonPatch(listOf(AddOperation(JsonPointer("/version"), TextNode(version))))
        return mapOf("content" to jacksonObjectMapper().writeValueAsString(jsonPatch))
    }

    fun redeploy(
        token: String,
        details: ApplicationDeploymentDetailsResource,
        applyLink: String
    ): JsonNode {
        val payload = ApplyPayload(listOf(details.applicationDeploymentCommand.applicationDeploymentRef))
        return booberWebClient.put<JsonNode>(token, applyLink, body = payload).toMono().blockNonNullWithTimeout()
    }

    private fun <T> Mono<T>.blockNonNullWithTimeout() =
        this.blockNonNullAndHandleError(Duration.ofSeconds(30), "boober")
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
