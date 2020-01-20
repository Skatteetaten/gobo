package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import java.util.concurrent.CompletableFuture
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaService
import no.skatteetaten.aurora.gobo.integration.dbh.JdbcUser
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.multipleKeysLoader
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaQueryResolver(private val databaseSchemaService: DatabaseSchemaService) :
    GraphQLQueryResolver {

    fun databaseSchemas(affiliations: List<String>, dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schemas")

        return affiliations.flatMap { affiliation ->
            databaseSchemaService.getDatabaseSchemas(affiliation)
                .map { DatabaseSchema.create(it, Affiliation(affiliation)) }
        }
    }

    fun restorableDatabaseSchemas(
        affiliations: List<String>,
        dfe: DataFetchingEnvironment
    ): List<RestorableDatabaseSchema> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schemas")

        return affiliations.flatMap { affiliation ->
            databaseSchemaService.getRestorableDatabaseSchemas(affiliation).map {
                RestorableDatabaseSchema(
                    it.setToCooldownAtInstant,
                    it.deleteAfterInstant,
                    DatabaseSchema.create(it.databaseSchema, Affiliation(affiliation))
                )
            }
        }
    }

    fun databaseSchema(id: String, dfe: DataFetchingEnvironment): DatabaseSchema? {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schema")

        val databaseSchema = databaseSchemaService.getDatabaseSchema(id) ?: return null
        return DatabaseSchema.create(databaseSchema, Affiliation(databaseSchema.affiliation))
    }
}

@Component
class DatabaseSchemaMutationResolver(private val databaseSchemaService: DatabaseSchemaService) :
    GraphQLMutationResolver {
    fun updateDatabaseSchema(input: UpdateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot update database schema")
        return databaseSchemaService.updateDatabaseSchema(input.toSchemaUpdateRequest())
            .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    }

    fun deleteDatabaseSchemas(
        input: DeleteDatabaseSchemasInput,
        dfe: DataFetchingEnvironment
    ): DeleteDatabaseSchemasResponse {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot delete database schemas")
        val responses = databaseSchemaService.deleteDatabaseSchemas(input.toSchemaDeletionRequests())
        return DeleteDatabaseSchemasResponse.create(responses)
    }

    fun testJdbcConnectionForJdbcUser(input: JdbcUser, dfe: DataFetchingEnvironment): Boolean {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot test jdbc connection")
        return databaseSchemaService.testJdbcConnection(input)
    }

    fun testJdbcConnectionForId(id: String, dfe: DataFetchingEnvironment): Boolean {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot test jdbc connection")
        return databaseSchemaService.testJdbcConnection(id)
    }

    fun createDatabaseSchema(input: CreateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot create database schema")
        return databaseSchemaService.createDatabaseSchema(input.toSchemaCreationRequest())
            .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    }
}

@Component
class DatabaseSchemaResolver(val applicationService: ApplicationServiceBlocking) : GraphQLResolver<DatabaseSchema> {

    fun applicationDeployments(
        schema: DatabaseSchema,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<List<ApplicationDeployment>> =
        dfe.multipleKeysLoader(DatabaseSchemaDataLoader::class).load(schema.id)
}
