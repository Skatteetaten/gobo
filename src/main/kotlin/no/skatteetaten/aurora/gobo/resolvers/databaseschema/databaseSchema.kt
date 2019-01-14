package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import java.time.Instant

data class DatabaseUser(val username: String, val password: String, val type: String)

data class DatabaseSchema(
    val id: String,
    val type: String,
    val jdbcUrl: String,
    val name: String,
    val affiliation: Affiliation,
    val databaseEngine: String,
    val applicationDeployment: ApplicationDeployment,
    val createdBy: String,
    val createdDate: Instant,
    val lastUsedDate: Instant,
    val sizeInMb: Double,
    val users: List<DatabaseUser>
)