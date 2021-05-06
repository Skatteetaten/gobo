package no.skatteetaten.aurora.gobo.graphql.database

import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.graphql.pageEdges
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaQuery(val databaseService: DatabaseService) : Query {

    suspend fun databaseInstances(affiliation: String? = null, dfe: DataFetchingEnvironment): List<DatabaseInstance> {
        dfe.checkValidUserToken()
        return databaseService
            .getDatabaseInstances()
            .map { DatabaseInstance.create(it) }
            .filter { databaseInstance ->
                affiliation?.let { it == databaseInstance.affiliation?.name } ?: true
            }
    }

    suspend fun databaseSchema(id: String, dfe: DataFetchingEnvironment): DatabaseSchema? {
        dfe.checkValidUserToken()
        val databaseSchema = databaseService.getDatabaseSchema(id)
        return DatabaseSchema.create(databaseSchema, Affiliation(databaseSchema.affiliation))
    }

    suspend fun databaseSchemas(
        affiliations: List<String>,
        first: Int,
        after: String? = null,
        dfe: DataFetchingEnvironment
    ): DatabaseSchemaConnection {
        dfe.checkValidUserToken()
        val schemas = affiliations
            .flatMap { affiliation ->
                databaseService.getDatabaseSchemas(affiliation)
                    .map { DatabaseSchema.create(it, Affiliation(affiliation)) }
            }.map { DatabaseSchemaEdge(it) }.sortedBy { it.node.name }

        val pagedSchemas = pageEdges(schemas, first, after)
        return DatabaseSchemaConnection(
            edges = pagedSchemas.edges,
            pageInfo = pagedSchemas.pageInfo,
            totalCount = schemas.size
        )
    }

    suspend fun restorableDatabaseSchemas(
        affiliations: List<String>,
        dfe: DataFetchingEnvironment
    ): List<RestorableDatabaseSchema> {
        dfe.checkValidUserToken()
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
