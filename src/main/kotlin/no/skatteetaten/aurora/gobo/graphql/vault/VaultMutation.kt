package no.skatteetaten.aurora.gobo.graphql.vault

import org.springframework.stereotype.Component
import com.expediagroup.graphql.spring.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.VaultService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken

@Component
class VaultMutation(val vaultService: VaultService) : Mutation {

    suspend fun createVault(input: VaultCreationInput, dfe: DataFetchingEnvironment): Vault {
        dfe.checkValidUserToken()
        return vaultService.createVault(dfe.token(), input)
    }

    suspend fun deleteVault(input: DeleteVaultInput, dfe: DataFetchingEnvironment): DeleteVaultResponse {
        vaultService.deleteVault(dfe.token(), input.affiliationName, input.vaultName)
        return DeleteVaultResponse(input.affiliationName, input.vaultName)
    }
}
