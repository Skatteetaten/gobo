package no.skatteetaten.aurora.gobo.resolvers.database

import graphql.schema.DataFetchingEnvironment
import java.time.Instant
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseInstanceResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseUserResource
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaCooldownChangeResponse
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaCreationRequest
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaDeletionRequest
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaRestorationRequest
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaUpdateRequest
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment

data class Label(val key: String, val value: String)

data class DatabaseInstance(
    val engine: String,
    val instanceName: String,
    val host: String,
    val port: Int,
    val createSchemaAllowed: Boolean,
    val affiliation: Affiliation?,
    val labels: List<Label>
) {
    companion object {
        fun create(databaseInstanceResource: DatabaseInstanceResource) =
            DatabaseInstance(
                engine = databaseInstanceResource.engine,
                instanceName = databaseInstanceResource.instanceName,
                host = databaseInstanceResource.host,
                port = databaseInstanceResource.port,
                createSchemaAllowed = databaseInstanceResource.createSchemaAllowed,
                affiliation = databaseInstanceResource.labels["affiliation"]?.let { Affiliation(it) },
                labels = databaseInstanceResource.labels.map { Label(it.key, it.value) }
            )
    }
}

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
    val engine: String,
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
                engine = databaseSchema.databaseInstance.engine,
                createdBy = databaseSchema.createdBy,
                createdDate = databaseSchema.createdDateAsInstant(),
                lastUsedDate = databaseSchema.lastUsedDateAsInstant(),
                sizeInMb = databaseSchema.metadata.sizeInMb,
                users = databaseSchema.users.map { DatabaseUser.create(it) }
            )
    }
    suspend fun applicationDeployments(dfe: DataFetchingEnvironment): List<ApplicationDeployment> {
//        dfe.loadMany<>() TODO; use dataloader
        return emptyList()
    }
}

data class JdbcUser(
    val username: String,
    val password: String,
    val jdbcUrl: String
)

data class RestorableDatabaseSchema(
    val setToCooldownAt: Instant,
    val deleteAfter: Instant,
    val databaseSchema: DatabaseSchema
)

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
    val jdbcUser: JdbcUser? = null,
    val engine: String,
    val instanceName: String? = null
) {
    fun toSchemaCreationRequest() =
        SchemaCreationRequest(
            jdbcUser = jdbcUser,
            engine = engine,
            instanceName = instanceName,
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

data class CooldownChangeDatabaseSchemasResponse(
    val succeeded: List<String> = emptyList(),
    val failed: List<String> = emptyList()
) {
    companion object {
        fun create(responses: List<SchemaCooldownChangeResponse>) =
            CooldownChangeDatabaseSchemasResponse(
                succeeded = responses.filter { it.success }.map { it.id },
                failed = responses.filter { !it.success }.map { it.id }
            )
    }
}

data class RestoreDatabaseSchemasInput(val ids: List<String>, val active: Boolean) {
    fun toSchemaRestorationRequests() = ids.map { SchemaRestorationRequest(it, active) }
}

data class ConnectionVerificationResponse(
    val hasSucceeded: Boolean = false,
    val message: String = ""
)
