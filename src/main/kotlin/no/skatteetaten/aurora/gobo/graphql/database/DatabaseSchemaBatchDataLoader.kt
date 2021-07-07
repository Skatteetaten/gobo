package no.skatteetaten.aurora.gobo.graphql.database

import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaBatchDataLoader(private val databaseService: DatabaseService) : GoboDataLoader<String, List<DatabaseSchema>>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GoboGraphQLContext): Map<String, List<DatabaseSchema>> {
        return keys.associateWith { affiliation ->
            databaseService.getDatabaseSchemas(affiliation)
                .map { schema -> DatabaseSchema.create(schema, Affiliation(affiliation)) }
        }
    }
}
