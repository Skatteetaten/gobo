package no.skatteetaten.aurora.gobo.graphql.vault

import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.VaultContext
import no.skatteetaten.aurora.gobo.integration.boober.VaultService
import no.skatteetaten.aurora.gobo.security.ifValidUserToken
import org.springframework.stereotype.Component

@Component
class VaultMutation(val vaultService: VaultService) : Mutation {

    suspend fun createVault(input: CreateVaultInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.createVault(
                ctx = VaultContext(
                    token = dfe.token,
                    affiliationName = input.affiliationName,
                    vaultName = input.vaultName
                ),
                permissions = input.permissions,
                secrets = input.secrets
            ).let { Vault.create(it) }
        }

    suspend fun renameVault(input: RenameVaultInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.renameVault(
                oldVaultCtx = VaultContext(
                    token = dfe.token,
                    affiliationName = input.affiliationName,
                    vaultName = input.vaultName
                ),
                newVaultName = input.newVaultName
            ).let { Vault.create(it) }
        }

    suspend fun deleteVault(input: DeleteVaultInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.deleteVault(VaultContext(dfe.token, input.affiliationName, input.vaultName))
            DeleteVaultResponse(input.affiliationName, input.vaultName)
        }

    suspend fun addVaultPermissions(input: AddVaultPermissionsInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.addVaultPermissions(
                ctx = VaultContext(dfe.token, input.affiliationName, input.vaultName),
                permissions = input.permissions
            ).let { Vault.create(it) }
        }

    suspend fun removeVaultPermissions(input: RemoveVaultPermissionsInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.removeVaultPermissions(
                ctx = VaultContext(dfe.token, input.affiliationName, input.vaultName),
                permissions = input.permissions
            ).let { Vault.create(it) }
        }

    suspend fun addVaultSecrets(input: AddVaultSecretsInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.addVaultSecrets(
                ctx = VaultContext(dfe.token, input.affiliationName, input.vaultName),
                secrets = input.secrets
            ).let { Vault.create(it) }
        }

    suspend fun removeVaultSecrets(input: RemoveVaultSecretsInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.removeVaultSecrets(
                ctx = VaultContext(dfe.token, input.affiliationName, input.vaultName),
                secretNames = input.secretNames
            ).let { Vault.create(it) }
        }

    suspend fun renameVaultSecret(input: RenameVaultSecretInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.renameVaultSecret(
                ctx = VaultContext(dfe.token, input.affiliationName, input.vaultName),
                secretName = input.secretName,
                newSecretName = input.newSecretName
            ).let { Vault.create(it) }
        }

    suspend fun updateVaultSecret(input: UpdateVaultSecretInput, dfe: DataFetchingEnvironment) =
        dfe.ifValidUserToken {
            vaultService.updateVaultSecret(
                ctx = VaultContext(dfe.token, input.affiliationName, input.vaultName),
                secretName = input.secretName,
                content = input.base64Content
            ).let { Vault.create(it) }
        }
}
