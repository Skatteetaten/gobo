package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import java.time.Instant
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseUserResource
import no.skatteetaten.aurora.gobo.integration.dbh.JdbcUser
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaCreationRequest
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaDeletionRequest
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaDeletionResponse
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaUpdateRequest
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation

data class DatabaseUser(val username: String, val password: String, val type: String) {
    companion object {
        fun create(userResource: DatabaseUserResource) =
            DatabaseUser(username = userResource.username, password = userResource.password, type = userResource.type)
    }
}

data class DatabaseSchema(
    val id: String,
    val type: String,
    val jdbcUrl: String,
    val name: String,
    val environment: String,
    val application: String,
    val discriminator: String,
    val description: String?,
    val affiliation: Affiliation,
    val databaseEngine: String,
    val createdBy: String?,
    val createdDate: Instant,
    val lastUsedDate: Instant?,
    val sizeInMb: Double,
    val users: List<DatabaseUser>
) {
    companion object {
        fun create(databaseSchema: DatabaseSchemaResource, affiliation: Affiliation) =
            DatabaseSchema(
                id = databaseSchema.id,
                type = databaseSchema.type,
                jdbcUrl = databaseSchema.jdbcUrl,
                name = databaseSchema.name,
                environment = databaseSchema.environment,
                application = databaseSchema.application,
                discriminator = databaseSchema.discriminator,
                description = databaseSchema.description,
                affiliation = affiliation,
                databaseEngine = databaseSchema.databaseInstance.engine,
                createdBy = databaseSchema.createdBy,
                createdDate = databaseSchema.createdDateAsInstant(),
                lastUsedDate = databaseSchema.lastUsedDateAsInstant(),
                sizeInMb = databaseSchema.metadata.sizeInMb,
                users = databaseSchema.users.map { DatabaseUser.create(it) }
            )
    }
}

data class UpdateDatabaseSchemaInput(
    val discriminator: String,
    val createdBy: String,
    val description: String,
    val id: String,
    val affiliation: String,
    val application: String,
    val environment: String
) {
    fun toSchemaUpdateRequest() =
        SchemaUpdateRequest(
            id = id,
            labels = mapOf(
                "description" to description,
                "name" to discriminator,
                "userId" to createdBy,
                "affiliation" to affiliation,
                "application" to application,
                "environment" to environment
            )
        )
}

data class CreateDatabaseSchemaInput(
    val discriminator: String,
    val createdBy: String,
    val description: String,
    val affiliation: String,
    val application: String,
    val environment: String,
    val jdbcUser: JdbcUser? = null
) {
    fun toSchemaCreationRequest() =
        SchemaCreationRequest(
            jdbcUser = jdbcUser,
            labels = mapOf(
                "description" to description,
                "name" to discriminator,
                "userId" to createdBy,
                "affiliation" to affiliation,
                "application" to application,
                "environment" to environment
            )
        )
}

data class DeleteDatabaseSchemasInput(val ids: List<String>) {
    fun toSchemaDeletionRequests() = ids.map { SchemaDeletionRequest(it) }
}

data class DeleteDatabaseSchemasResponse(
    val succeeded: List<String> = emptyList(),
    val failed: List<String> = emptyList()
) {
    companion object {
        fun create(responses: List<SchemaDeletionResponse>) =
            DeleteDatabaseSchemasResponse(
                succeeded = responses.filter { it.success }.map { it.id },
                failed = responses.filter { !it.success }.map { it.id }
            )
    }
}
