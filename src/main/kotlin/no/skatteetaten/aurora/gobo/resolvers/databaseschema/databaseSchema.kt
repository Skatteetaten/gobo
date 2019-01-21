package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseUserResource
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import java.time.Instant

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
    val appDbName: String,
    val affiliation: Affiliation,
    val databaseEngine: String,
    val applicationDeployment: ApplicationDeployment?,
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
                appDbName = databaseSchema.appDbName,
                affiliation = affiliation,
                databaseEngine = databaseSchema.databaseInstance.engine,
                applicationDeployment = null,
                createdBy = databaseSchema.createdBy,
                createdDate = databaseSchema.createdDateAsInstant(),
                lastUsedDate = databaseSchema.lastUsedDateAsInstant(),
                sizeInMb = databaseSchema.metadata.sizeInMb,
                users = databaseSchema.users.map { DatabaseUser.create(it) }
            )
    }
}