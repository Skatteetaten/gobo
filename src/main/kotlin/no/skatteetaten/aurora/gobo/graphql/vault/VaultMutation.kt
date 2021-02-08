package no.skatteetaten.aurora.gobo.graphql.vault

import org.springframework.stereotype.Component
import com.expediagroup.graphql.spring.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.VaultService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken

@Component
class VaultMutation(val vaultService: VaultService) : Mutation {

    suspend fun createVault(input: CreateVaultInput, dfe: DataFetchingEnvironment): Vault {
        dfe.checkValidUserToken()
        return vaultService.createVault(dfe.token(), input).let { Vault.create(it) }
    }

    suspend fun deleteVault(input: DeleteVaultInput, dfe: DataFetchingEnvironment): DeleteVaultResponse {
        dfe.checkValidUserToken()
        vaultService.deleteVault(dfe.token(), input.affiliationName, input.vaultName)
        return DeleteVaultResponse(input.affiliationName, input.vaultName)
    }

    suspend fun addVaultPermissions(input: AddVaultPermissionsInput, dfe: DataFetchingEnvironment): Vault {
        dfe.checkValidUserToken()
        return vaultService.addVaultPermissions(dfe.token(), input.affiliationName, input.vaultName, input.permissions)
            .let {
                Vault.create(it)
            }
    }

    suspend fun removeVaultPermissions(input: RemoveVaultPermissionsInput, dfe: DataFetchingEnvironment): Vault {
        dfe.checkValidUserToken()
        return vaultService.removeVaultPermissions(
            dfe.token(),
            input.affiliationName,
            input.vaultName,
            input.permissions
        )
            .let { Vault.create(it) }
    }

    suspend fun addVaultSecrets(input: AddVaultSecretsInput, dfe: DataFetchingEnvironment): Vault {
        dfe.checkValidUserToken()
        return vaultService.addVaultSecrets(dfe.token(), input.affiliationName, input.vaultName, input.secrets)
            .let { Vault.create(it) }
    }

    suspend fun removeVaultSecrets(input: RemoveVaultSecretsInput, dfe: DataFetchingEnvironment): Vault {
        dfe.checkValidUserToken()
        return vaultService.removeVaultSecrets(dfe.token(), input.affiliationName, input.vaultName, input.secrets)
            .let { Vault.create(it) }
    }
}
