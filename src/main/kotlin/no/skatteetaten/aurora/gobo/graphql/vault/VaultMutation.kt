package no.skatteetaten.aurora.gobo.graphql.vault

import org.springframework.stereotype.Component
import com.expediagroup.graphql.spring.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.graphql.database.ConnectionVerificationResponse
import no.skatteetaten.aurora.gobo.graphql.database.CreateDatabaseSchemaInput
import no.skatteetaten.aurora.gobo.graphql.database.DatabaseSchema
import no.skatteetaten.aurora.gobo.graphql.database.JdbcUser
import no.skatteetaten.aurora.gobo.integration.boober.VaultService
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken

@Component
class VaultMutation(val vaultService: VaultService) : Mutation {

    // suspend fun createVault(input: CreateDatabaseSchemaInput, dfe: DataFetchingEnvironment): DatabaseSchema {
    //     dfe.checkValidUserToken()
    //     return databaseService.createDatabaseSchema(input.toSchemaCreationRequest())
    //         .let { DatabaseSchema.create(it, Affiliation(it.affiliation)) }
    // }
}