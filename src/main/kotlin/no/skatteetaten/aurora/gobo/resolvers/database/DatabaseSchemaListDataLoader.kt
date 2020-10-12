package no.skatteetaten.aurora.gobo.resolvers.database

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaResource
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaListDataLoader(val databaseSchemaServiceReactive: DatabaseServiceReactive) :
    KeyDataLoader<String, List<DatabaseSchemaResource>> {

    override suspend fun getByKey(affiliation: String, context: GoboGraphQLContext): List<DatabaseSchemaResource> =
        databaseSchemaServiceReactive.getDatabaseSchemas(affiliation)
}
