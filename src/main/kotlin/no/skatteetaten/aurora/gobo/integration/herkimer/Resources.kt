package no.skatteetaten.aurora.gobo.integration.herkimer

import java.time.LocalDateTime
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

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
    PostgresDatabaseInstance, StorageGridTenant, MinioPolicy
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
    val id: String,
    val name: String,
    val kind: ResourceKind,
    val ownerId: String,
    val claims: List<ResourceClaim>? = null,
    val active: Boolean,
    val setToCooldownAt: LocalDateTime?,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val createdBy: String,
    val modifiedBy: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceClaim(
    val id: String,
    val ownerId: String,
    val resourceId: Int,
    val credentials: ObjectNode,
    val name: String,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val createdBy: String,
    val modifiedBy: String
)
