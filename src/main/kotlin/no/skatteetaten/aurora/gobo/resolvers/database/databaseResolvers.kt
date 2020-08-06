package no.skatteetaten.aurora.gobo.resolvers.database

/*
@Component
class DatabaseSchemaQueryResolver(private val databaseService: DatabaseService) :
    GraphQLQueryResolver {

    fun databaseSchemas(affiliations: List<String>, dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schemas")

        return affiliations.flatMap { affiliation ->
            databaseService.getDatabaseSchemas(affiliation)
                .map { DatabaseSchema.create(it, Affiliation(affiliation)) }
        }
    }

    fun databaseSchema(id: String, dfe: DataFetchingEnvironment): DatabaseSchema? {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schema")

        val databaseSchema = databaseService.getDatabaseSchema(id) ?: return null
        return DatabaseSchema.create(databaseSchema, Affiliation(databaseSchema.affiliation))
    }

    fun databaseInstances(affiliation: String?, dfe: DataFetchingEnvironment): List<DatabaseInstance> {
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
    fun updateDatabaseSchema(input: UpdateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot update database schema")
        return databaseService.updateDatabaseSchema(input.toSchemaUpdateRequest())
            .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    }

    fun deleteDatabaseSchemas(
        input: DeleteDatabaseSchemasInput,
        dfe: DataFetchingEnvironment
    ): DeleteDatabaseSchemasResponse {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot delete database schemas")
        val responses = databaseService.deleteDatabaseSchemas(input.toSchemaDeletionRequests())
        return DeleteDatabaseSchemasResponse.create(responses)
    }

    fun testJdbcConnectionForJdbcUser(input: JdbcUser, dfe: DataFetchingEnvironment): ConnectionVerificationResponse {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot test jdbc connection")
        return databaseService.testJdbcConnection(input)
    }

    fun testJdbcConnectionForId(id: String, dfe: DataFetchingEnvironment): ConnectionVerificationResponse {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot test jdbc connection")
        return databaseService.testJdbcConnection(id)
    }

    fun createDatabaseSchema(input: CreateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
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
