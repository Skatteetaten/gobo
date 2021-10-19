package no.skatteetaten.aurora.gobo.integration.herkimer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.RequiresHerkimer
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.integration.postOrNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

private val logger = KotlinLogging.logger {}

@Service
@ConditionalOnBean(RequiresHerkimer::class)
class HerkimerServiceReactive(
    @TargetService(ServiceTypes.HERKIMER) private val webClient: WebClient,
    val mapper: ObjectMapper
) : HerkimerService {
    override suspend fun registerResourceAndClaim(
        registerAndClaimCommand: RegisterResourceAndClaimCommand
    ): HerkimerResult {
        val response: AuroraResponse<ResourceHerkimer, ErrorResponse>? =
            webClient.postOrNull(
                body = registerAndClaimCommand.toResourcePayload(),
                uri = "/resource"
            )

        if (response?.success != true) {
            return response.also {
                logger.warn { "Unable to register resourceKind=${registerAndClaimCommand.resourceKind} payload=${registerAndClaimCommand.toResourcePayload()} errorMessage=${response?.message ?: "no message"}" }
            }.toHerkimerResult()
        }

        val resourceId = response.items.first().id

        val result: AuroraResponse<JsonNode, ErrorResponse>? =
            webClient.postOrNull(
                body = registerAndClaimCommand.toClaimPayload(),
                uri = "/resource/{resourceId}/claims",
                resourceId
            )

        return result.logWarnIfFailure(
            resourceKind = registerAndClaimCommand.resourceKind,
            resourceId = resourceId,
            resourceName = registerAndClaimCommand.resourceName
        ).toHerkimerResult()
    }

    private fun RegisterResourceAndClaimCommand.toResourcePayload() =
        ResourcePayload(name = resourceName, kind = resourceKind, ownerId = ownerId)

    private fun RegisterResourceAndClaimCommand.toClaimPayload() =
        ResourceClaimPayload(ownerId = ownerId, credentials = mapper.convertValue(credentials), name = claimName)
}

private fun AuroraResponse<*, *>?.logWarnIfFailure(
    resourceKind: ResourceKind,
    resourceId: String,
    resourceName: String
): AuroraResponse<*, *>? {
    if (this?.success != true) {
        val message =
            "Unable to claim registered resourceKind=$resourceKind for resourceId=$resourceId " +
                "resourceName=$resourceName errorMessage=$messageOrDefault"

        logger.warn { message }
    }

    return this
}

private val AuroraResponse<*, *>?.messageOrDefault: String
    get() = this?.message ?: "Did not receive a message from herkimer."

private fun AuroraResponse<*, *>?.toHerkimerResult(): HerkimerResult =
    HerkimerResult(
        this?.success ?: false
    )

data class HerkimerResult(
    val success: Boolean
)

interface CredentialBase

interface HerkimerService {
    suspend fun registerResourceAndClaim(registerAndClaimCommand: RegisterResourceAndClaimCommand): HerkimerResult =
        integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Herkimer integration is disabled for this environment")
}

@Service
@ConditionalOnMissingBean(RequiresHerkimer::class)
class HerkimerServiceDisabled : HerkimerService

data class RegisterResourceAndClaimCommand(
    val ownerId: String,
    val credentials: CredentialBase,
    val resourceName: String,
    val claimName: String,
    val resourceKind: ResourceKind
)

data class ResourcePayload(
    val name: String,
    val kind: ResourceKind,
    val ownerId: String,
)

data class ResourceClaimPayload(
    val ownerId: String,
    val credentials: JsonNode,
    val name: String
)

enum class ResourceKind {
    PostgresDatabaseInstance, StorageGridTenant
}

data class AuroraResponse<Item, Error>(
    val success: Boolean = true,
    val message: String = "OK",
    val items: List<Item> = emptyList(),
    val errors: List<Error> = emptyList(),
    val count: Int = items.size + errors.size
)

data class ErrorResponse(val errorMessage: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceHerkimer(
    val id: String
)
