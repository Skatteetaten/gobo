package no.skatteetaten.aurora.gobo.resolvers.database

import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
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

@Component
class DatabaseSchemaMutation(val databaseService: DatabaseServiceReactive) : Mutation {

    suspend fun testJdbcConnectionForJdbcUser(
        input: JdbcUser,
        dfe: DataFetchingEnvironment
    ): ConnectionVerificationResponse {
// FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot test jdbc connection")
        return databaseService.testJdbcConnection(user = input)
    }

    suspend fun testJdbcConnectionForId(id: String, dfe: DataFetchingEnvironment): ConnectionVerificationResponse {
// FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot test jdbc connection")
        return databaseService.testJdbcConnection(id = id)
    }

    suspend fun createDatabaseSchema(input: CreateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        // FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot create database schema")
        return databaseService.createDatabaseSchema(input.toSchemaCreationRequest())
            .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    }

    suspend fun updateDatabaseSchema(input: UpdateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        // FIXME: if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot update database schema")
        return databaseService.updateDatabaseSchema(input.toSchemaUpdateRequest())
            .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    }

    suspend fun databaseSchema(id: String, dfe: DataFetchingEnvironment): DatabaseSchema? {
// FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schema")

        val databaseSchema = databaseService.getDatabaseSchema(id)
        return DatabaseSchema.create(databaseSchema, Affiliation(databaseSchema.affiliation))
    }

    suspend fun deleteDatabaseSchemas(
        input: DeleteDatabaseSchemasInput,
        dfe: DataFetchingEnvironment
    ): CooldownChangeDatabaseSchemasResponse {
        // FIXME: if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot delete database schemas")
        val responses = databaseService.deleteDatabaseSchemas(input.toSchemaDeletionRequests())
        return CooldownChangeDatabaseSchemasResponse.create(responses)
    }

    suspend fun restoreDatabaseSchemas(
        input: RestoreDatabaseSchemasInput,
        dfe: DataFetchingEnvironment
    ): CooldownChangeDatabaseSchemasResponse {
        // FIXME: if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot restore database schemas")
        val responses = databaseService.restoreDatabaseSchemas(input.toSchemaRestorationRequests())
        return CooldownChangeDatabaseSchemasResponse.create(responses)
    }
}
