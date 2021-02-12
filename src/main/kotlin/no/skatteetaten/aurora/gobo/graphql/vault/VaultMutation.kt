package no.skatteetaten.aurora.gobo.graphql.vault

import org.springframework.stereotype.Component
import com.expediagroup.graphql.spring.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.VaultService
import no.skatteetaten.aurora.gobo.security.ifValidUserToken

@Component
class VaultMutation(val vaultService: VaultService) : Mutation {

    suspend fun createVault(input: CreateVaultInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken { vaultService.createVault(dfe.token(), input).let { Vault.create(it) } }

    suspend fun deleteVault(input: DeleteVaultInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.deleteVault(dfe.token(), input.affiliationName, input.vaultName)
            DeleteVaultResponse(input.affiliationName, input.vaultName)
        }

    suspend fun addVaultPermissions(input: AddVaultPermissionsInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.addVaultPermissions(
                token = dfe.token(),
                affiliationName = input.affiliationName,
                vaultName = input.vaultName,
                permissions = input.permissions
            ).let { Vault.create(it) }
        }

    suspend fun removeVaultPermissions(input: RemoveVaultPermissionsInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.removeVaultPermissions(
                token = dfe.token(),
                affiliationName = input.affiliationName,
                vaultName = input.vaultName,
                permissions = input.permissions
            ).let { Vault.create(it) }
        }

    suspend fun addVaultSecrets(input: AddVaultSecretsInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.addVaultSecrets(dfe.token(), input.affiliationName, input.vaultName, input.secrets)
                .let { Vault.create(it) }
        }

    suspend fun removeVaultSecrets(input: RemoveVaultSecretsInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.removeVaultSecrets(dfe.token(), input.affiliationName, input.vaultName, input.secrets)
                .let { Vault.create(it) }
        }
}
