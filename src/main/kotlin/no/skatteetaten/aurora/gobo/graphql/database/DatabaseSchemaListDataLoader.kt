package no.skatteetaten.aurora.gobo.graphql.database

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaListDataLoader(val databaseService: DatabaseService) :
    KeyDataLoader<String, List<DatabaseSchema>> {

    override suspend fun getByKey(affiliation: String, context: GoboGraphQLContext): List<DatabaseSchema> =
        databaseService.getDatabaseSchemas(affiliation).map { DatabaseSchema.create(it, Affiliation(affiliation)) }
}
