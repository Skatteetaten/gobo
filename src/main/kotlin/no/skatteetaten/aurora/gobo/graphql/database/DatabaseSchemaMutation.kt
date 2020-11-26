package no.skatteetaten.aurora.gobo.graphql.database

import com.expediagroup.graphql.spring.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaMutation(val databaseService: DatabaseService) : Mutation {

    suspend fun testJdbcConnectionForJdbcUser(
        input: JdbcUser,
        dfe: DataFetchingEnvironment
    ): ConnectionVerificationResponse {
        dfe.checkValidUserToken()
        return databaseService.testJdbcConnection(user = input)
    }

    suspend fun testJdbcConnectionForId(id: String, dfe: DataFetchingEnvironment): ConnectionVerificationResponse {
        dfe.checkValidUserToken()
        return databaseService.testJdbcConnection(id = id)
    }

    suspend fun createDatabaseSchema(input: CreateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        dfe.checkValidUserToken()
        return databaseService.createDatabaseSchema(input.toSchemaCreationRequest())
            .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    }

    suspend fun updateDatabaseSchema(input: UpdateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
        dfe.checkValidUserToken()
        return databaseService.updateDatabaseSchema(input.toSchemaUpdateRequest())
            .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    }

    suspend fun databaseSchema(id: String, dfe: DataFetchingEnvironment): DatabaseSchema? {
        dfe.checkValidUserToken()
        val databaseSchema = databaseService.getDatabaseSchema(id)
        return DatabaseSchema.create(databaseSchema, Affiliation(databaseSchema.affiliation))
    }

    suspend fun deleteDatabaseSchemas(
        input: DeleteDatabaseSchemasInput,
        dfe: DataFetchingEnvironment
    ): CooldownChangeDatabaseSchemasResponse {
        dfe.checkValidUserToken()
        val responses = databaseService.deleteDatabaseSchemas(input.toSchemaDeletionRequests())
        return CooldownChangeDatabaseSchemasResponse.create(responses)
    }

    suspend fun restoreDatabaseSchemas(
        input: RestoreDatabaseSchemasInput,
        dfe: DataFetchingEnvironment
    ): CooldownChangeDatabaseSchemasResponse {
        dfe.checkValidUserToken()
        val responses = databaseService.restoreDatabaseSchemas(input.toSchemaRestorationRequests())
        return CooldownChangeDatabaseSchemasResponse.create(responses)
    }
}
