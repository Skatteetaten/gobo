package no.skatteetaten.aurora.gobo.integration.dbh

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.skatteetaten.aurora.gobo.graphql.database.JdbcUser
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseInstanceResource(
    val engine: String,
    val instanceName: String,
    val host: String,
    val port: Int,
    val createSchemaAllowed: Boolean,
    val labels: Map<String, String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseUserResource(val username: String, val password: String, val type: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseMetadataResource(val sizeInMb: Double)

interface ResourceValidator {
    val valid: Boolean
}

/**
 * labels:
 * createdBy == userId
 * discriminator == name
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseSchemaResource(
    val id: String,
    val type: String,
    val jdbcUrl: String,
    val name: String,
    val createdDate: Long,
    val lastUsedDate: Long?,
    val databaseInstance: DatabaseInstanceResource,
    val users: List<DatabaseUserResource>,
    val metadata: DatabaseMetadataResource,
    val labels: Map<String, String>
) : ResourceValidator {
    val environment: String by labels
    val application: String by labels
    val affiliation: String by labels
    val description: String? by labels.withDefault { null }

    private val userId: String by labels
    val createdBy: String
        get() = userId

    // Using filter because there is a collision on "name", property and label is called "name"
    val discriminator: String
        get() = labels.filter { it.key == "name" }.values.first()

    fun createdDateAsInstant(): Instant = Instant.ofEpochMilli(createdDate)

    fun lastUsedDateAsInstant(): Instant? =
        lastUsedDate?.let {
            return Instant.ofEpochMilli(it)
        }

    override val valid: Boolean
        get() = labels.containsKey("affiliation") &&
            labels.containsKey("userId") &&
            labels.containsKey("name") &&
            labels.containsKey("environment") &&
            labels.containsKey("application")
}

data class RestorableDatabaseSchemaResource(
    val databaseSchema: DatabaseSchemaResource,
    val setToCooldownAt: Long,
    val deleteAfter: Long
) : ResourceValidator {
    val setToCooldownAtInstant: Instant get() = Instant.ofEpochMilli(setToCooldownAt)
    val deleteAfterInstant: Instant get() = Instant.ofEpochMilli(deleteAfter)
    override val valid: Boolean get() = databaseSchema.valid
}

data class SchemaCreationRequest(
    val labels: Map<String, String>,
    @JsonProperty("schema")
    val jdbcUser: JdbcUser? = null,
    val engine: String,
    val instanceName: String? = null
) {
    private val requiredLabels = listOf(
        "affiliation",
        "name",
        "environment",
        "application"
    )

    fun findMissingOrEmptyLabels(): List<String> = requiredLabels.filter { labels[it]?.isEmpty() ?: true }
}

data class SchemaUpdateRequest(
    val id: String,
    val labels: Map<String, String>,
    @JsonProperty("schema")
    val jdbcUser: JdbcUser? = null
)

data class SchemaDeletionRequest(
    val id: String,
    val cooldownDurationHours: Long? = null
)

data class SchemaRestorationRequest(
    val id: String,
    val active: Boolean
)

data class DbhResponse<T>(val status: String, val items: List<T>, val totalCount: Int = items.size) {
    companion object {
        fun <T> ok(vararg items: T) = DbhResponse("OK", items.toList())
        fun <T> ok(item: T) = DbhResponse("OK", listOf(item))
        fun <T> ok() = DbhResponse("OK", emptyList<T>())
        fun failed(item: String) = DbhResponse("Failed", listOf(item))
        fun failed() = DbhResponse("Failed", emptyList<String>())
    }

    fun isOk() = status.lowercase() == "ok"
    fun isFailure() = status.lowercase() == "failed"
    fun isEmpty() = totalCount == 0
}

data class SchemaCooldownChangeResponse(val id: String, val success: Boolean)
