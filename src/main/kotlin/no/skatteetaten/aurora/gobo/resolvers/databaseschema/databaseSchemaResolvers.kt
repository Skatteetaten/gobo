package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceBlocking
import no.skatteetaten.aurora.gobo.integration.dbh.JdbcUser
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaDeletionRequest
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.multipleKeysLoader
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class DatabaseSchemaQueryResolver(
    private val databaseSchemaService: DatabaseSchemaServiceBlocking
) :
    GraphQLQueryResolver {

    fun databaseSchemas(affiliations: List<String>, dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schemas")

        return affiliations.flatMap { affiliation ->
            databaseSchemaService.getDatabaseSchemas(affiliation)
                .map { DatabaseSchema.create(it, Affiliation(affiliation)) }
        }
    }

    fun databaseSchema(id: String, dfe: DataFetchingEnvironment): DatabaseSchema? {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schema")

        val databaseSchema = databaseSchemaService.getDatabaseSchema(id) ?: return null
        return DatabaseSchema.create(databaseSchema, Affiliation(databaseSchema.affiliation))
    }
}

@Component
class DatabaseSchemaMutationResolver(private val databaseSchemaService: DatabaseSchemaServiceBlocking) :
    GraphQLMutationResolver {
    fun updateDatabaseSchema(input: UpdateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot update database schema")
        return databaseSchemaService.updateDatabaseSchema(input.toSchemaUpdateRequest())
            .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    }

    fun deleteDatabaseSchema(input: SchemaDeletionRequest, dfe: DataFetchingEnvironment): Boolean {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot delete database schema")
        return databaseSchemaService.deleteDatabaseSchema(input)
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