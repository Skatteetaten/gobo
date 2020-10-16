package no.skatteetaten.aurora.gobo.graphql.database

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaQuery(val databaseService: DatabaseServiceReactive) : Query {

    suspend fun databaseInstances(affiliation: String?, dfe: DataFetchingEnvironment): List<DatabaseInstance> {
        // FIXME: not anonymous user
        return databaseService
            .getDatabaseInstances()
            .map { DatabaseInstance.create(it) }
            .filter { databaseInstance ->
                affiliation?.let { it == databaseInstance.affiliation?.name } ?: true
            }
    }

    suspend fun databaseSchemas(affiliations: List<String>, dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        // FIXME: not anonymous user
        return affiliations?.flatMap { affiliation ->
            databaseService.getDatabaseSchemas(affiliation)
                .map { DatabaseSchema.create(it, Affiliation(affiliation)) }
        }
    }

    suspend fun restorableDatabaseSchemas(
        affiliations: List<String>,
        dfe: DataFetchingEnvironment
    ): List<RestorableDatabaseSchema> {
// FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get restorable database schemas")

        return affiliations.flatMap { affiliation ->
            databaseService.getRestorableDatabaseSchemas(affiliation).map {
                RestorableDatabaseSchema(
                    it.setToCooldownAtInstant,
                    it.deleteAfterInstant,
                    DatabaseSchema.create(it.databaseSchema, Affiliation(affiliation))
                )
            }
        }
    }
}
