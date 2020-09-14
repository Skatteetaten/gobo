package no.skatteetaten.aurora.gobo.resolvers.database

import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import org.springframework.stereotype.Component
import reactor.kotlin.core.publisher.toMono

@Component
class DatabaseSchemaQuery(val databaseService: DatabaseServiceReactive) : Query {

    suspend fun databaseInstances(affiliation: String?, dfe: DataFetchingEnvironment): List<DatabaseInstance> {
        //FIXME: not anonymous user
        return databaseService
                .getDatabaseInstances().awaitFirst()
                .map { DatabaseInstance.create(it) }
                .filter { databaseInstance ->
                    affiliation?.let { it == databaseInstance.affiliation?.name } ?: true
                }
    }

    suspend fun databaseSchemas(affiliations: List<String>, dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        //FIXME: not anonymous user
        return affiliations?.flatMap { affiliation ->
            databaseService.getDatabaseSchemas(affiliation).awaitFirst()
                    .map { DatabaseSchema.create(it, Affiliation(affiliation)) }
        } ?: emptyList()
    }

    suspend fun restorableDatabaseSchemas(
            affiliations: List<String>,
            dfe: DataFetchingEnvironment
    ): List<RestorableDatabaseSchema> {
//FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get restorable database schemas")

        return affiliations.flatMap { affiliation ->
            databaseService.getRestorableDatabaseSchemas(affiliation).awaitFirst().map {
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

    suspend fun testJdbcConnectionForJdbcUser(input: JdbcUser, dfe: DataFetchingEnvironment): ConnectionVerificationResponse {
//FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot test jdbc connection")
        return databaseService.testJdbcConnection(user=input).awaitSingle()
    }

    suspend fun testJdbcConnectionForId(id: String, dfe: DataFetchingEnvironment): ConnectionVerificationResponse {
//FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot test jdbc connection")
        return databaseService.testJdbcConnection(id = id).awaitSingle()
    }

    suspend fun createDatabaseSchema(input: CreateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        //FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot create database schema")
        return databaseService.createDatabaseSchema(input.toSchemaCreationRequest())
                .let { DatabaseSchema.create(it.awaitSingle(), Affiliation(it.awaitSingle().affiliation)) }
    }

    suspend fun updateDatabaseSchema(input: UpdateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        //FIXME: if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot update database schema")
        return databaseService.updateDatabaseSchema(input.toSchemaUpdateRequest())
                .let { DatabaseSchema.create(it.awaitSingle(), Affiliation(it.awaitSingle().affiliation)) }
    }


    suspend fun databaseSchema(id: String, dfe: DataFetchingEnvironment): DatabaseSchema? {
//FIXME:        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schema")

        val databaseSchema = databaseService.getDatabaseSchema(id) ?: return null
        return DatabaseSchema.create(databaseSchema.awaitSingle(), Affiliation(databaseSchema.awaitSingle().affiliation))
    }


    suspend fun deleteDatabaseSchemas(
            input: DeleteDatabaseSchemasInput,
            dfe: DataFetchingEnvironment
    ): CooldownChangeDatabaseSchemasResponse {
        //FIXME: if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot delete database schemas")
        val responses = databaseService.deleteDatabaseSchemas(input.toSchemaDeletionRequests()).collectList().awaitSingle()
        return CooldownChangeDatabaseSchemasResponse.create(responses)
    }

    suspend fun restoreDatabaseSchemas(
            input: RestoreDatabaseSchemasInput,
            dfe: DataFetchingEnvironment
    ): CooldownChangeDatabaseSchemasResponse {
        //FIXME: if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot restore database schemas")
        val responses = databaseService.restoreDatabaseSchemas(input.toSchemaRestorationRequests()).collectList().awaitSingle()
        return CooldownChangeDatabaseSchemasResponse.create(responses)
    }
}





/* JB
Interface: Query
@Component
class DatabaseSchemaQueryResolver(private val databaseService: DatabaseService) :
    GraphQLQueryResolver {

    DONE fun databaseSchemas(affiliations: List<String>, dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schemas")

        return affiliations.flatMap { affiliation ->
            databaseService.getDatabaseSchemas(affiliation)
                .map { DatabaseSchema.create(it, Affiliation(affiliation)) }
        }
    }

    DONE fun restorableDatabaseSchemas(
        affiliations: List<String>,
        dfe: DataFetchingEnvironment
    ): List<RestorableDatabaseSchema> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get restorable database schemas")

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

    DONE fun databaseSchema(id: String, dfe: DataFetchingEnvironment): DatabaseSchema? {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schema")

        val databaseSchema = databaseService.getDatabaseSchema(id) ?: return null
        return DatabaseSchema.create(databaseSchema, Affiliation(databaseSchema.affiliation))
    }

    DONE fun databaseInstances(affiliation: String?, dfe: DataFetchingEnvironment): List<DatabaseInstance> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database instances")
        return databaseService
            .getDatabaseInstances()
            .map { DatabaseInstance.create(it) }
            .filter { databaseInstance ->
                affiliation?.let { it == databaseInstance.affiliation?.name } ?: true
            }
    }
}

@Component
class DatabaseSchemaMutationResolver(private val databaseService: DatabaseService) :
    GraphQLMutationResolver {
    DONE fun updateDatabaseSchema(input: UpdateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot update database schema")
        return databaseService.updateDatabaseSchema(input.toSchemaUpdateRequest())
            .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    }

    DONE fun deleteDatabaseSchemas(
        input: DeleteDatabaseSchemasInput,
        dfe: DataFetchingEnvironment
    ): CooldownChangeDatabaseSchemasResponse {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot delete database schemas")
        val responses = databaseService.deleteDatabaseSchemas(input.toSchemaDeletionRequests())
        return CooldownChangeDatabaseSchemasResponse.create(responses)
    }

    DONE fun restoreDatabaseSchemas(
        input: RestoreDatabaseSchemasInput,
        dfe: DataFetchingEnvironment
    ): CooldownChangeDatabaseSchemasResponse {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot restore database schemas")
        val responses = databaseService.restoreDatabaseSchemas(input.toSchemaRestorationRequests())
        return CooldownChangeDatabaseSchemasResponse.create(responses)
    }

    DONE fun testJdbcConnectionForJdbcUser(input: JdbcUser, dfe: DataFetchingEnvironment): ConnectionVerificationResponse {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot test jdbc connection")
        return databaseService.testJdbcConnection(input)
    }

    DONE fun testJdbcConnectionForId(id: String, dfe: DataFetchingEnvironment): ConnectionVerificationResponse {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot test jdbc connection")
        return databaseService.testJdbcConnection(id)
    }

    DONE fun createDatabaseSchema(input: CreateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot create database schema")
        return databaseService.createDatabaseSchema(input.toSchemaCreationRequest())
            .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    }
}

@Component
class DatabaseSchemaResolver(val applicationService: ApplicationServiceBlocking) : GraphQLResolver<DatabaseSchema> {

    fun applicationDeployments(
        schema: DatabaseSchema,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<List<ApplicationDeployment>> =
        dfe.multipleKeysLoader(DatabaseDataLoader::class).load(schema.id)
}
*/
