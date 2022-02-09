package no.skatteetaten.aurora.gobo.integration.boober

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.graphql.vault.Secret
import org.springframework.stereotype.Service

data class BooberVaultInput(
    val name: String,
    val permissions: List<String>?,
    val secrets: Map<String, String>?
)

data class BooberVault(
    val name: String,
    val hasAccess: Boolean,
    val permissions: List<String>?,
    val secrets: Map<String, String>?
) {
    fun findExistingPermissions(inputPermissions: List<String>) =
        inputPermissions.filter { permissions?.contains(it) ?: false }.takeIf { it.isNotEmpty() }

    fun findMissingPermissions(inputPermissions: List<String>) =
        (inputPermissions - (permissions ?: emptyList()).toSet()).takeIf { it.isNotEmpty() }

    fun findExistingSecret(secretName: String) =
        secrets?.takeIf { it.containsKey(secretName) }?.let { secretName }

    fun findExistingSecrets(inputSecrets: List<Secret>) =
        inputSecrets.filter { secrets?.containsKey(it.name) ?: false }.map { it.name }.takeIf { it.isNotEmpty() }

    fun findMissingSecret(secretName: String) =
        secrets?.takeIf { !it.containsKey(secretName) }?.let { secretName }

    fun findMissingSecrets(inputSecrets: List<String>) =
        (inputSecrets - (secrets?.keys ?: emptySet()).toSet()).takeIf { it.isNotEmpty() }

    fun toInput() = BooberVaultInput(name = name, permissions = permissions, secrets = secrets)
}

data class VaultContext(val token: String, val affiliationName: String, val vaultName: String)

private val logger = KotlinLogging.logger { }

@Service
class VaultService(private val booberWebClient: BooberWebClient) {

    /**
     * Get all vaults for the affiliation, even if the user does not have access to the vault
     */
    suspend fun getVaults(token: String, affiliationName: String) =
        booberWebClient.get<BooberVault>(token = token, url = "/v1/vault/$affiliationName").responses()

    /**
     * Get a vault, throws exception if the user does not have access to the vault
     */
    suspend fun getVault(ctx: VaultContext) =
        booberWebClient.get<BooberVault>(token = ctx.token, url = "/v1/vault/${ctx.affiliationName}/${ctx.vaultName}")
            .response()

    suspend fun createVault(ctx: VaultContext, permissions: List<String>, secrets: List<Secret>): BooberVault {
        checkIfVaultExists(ctx)
        return putVault(
            ctx.token,
            ctx.affiliationName,
            BooberVaultInput(ctx.vaultName, permissions, secrets.toBooberInput())
        )
    }

    suspend fun renameVault(oldVaultCtx: VaultContext, newVaultName: String): BooberVault {
        val oldBooberVault = getVault(oldVaultCtx)
        checkIfVaultExists(VaultContext(oldVaultCtx.token, oldVaultCtx.affiliationName, newVaultName))

        val newBooberVault = oldBooberVault.copy(name = newVaultName)
        val renamedVault = putVault(
            oldVaultCtx.token,
            oldVaultCtx.affiliationName,
            BooberVaultInput(
                newBooberVault.name,
                newBooberVault.permissions,
                newBooberVault.secrets?.map { it.key to it.value }?.toMap()
            )
        )
        deleteVault(oldVaultCtx)
        return renamedVault
    }

    suspend fun deleteVault(ctx: VaultContext) {
        getVault(ctx)
        val url = "/v1/vault/${ctx.affiliationName}/${ctx.vaultName}"
        booberWebClient.delete<BooberVault>(
            url = url,
            token = ctx.token
        ).responses()
    }

    suspend fun addVaultPermissions(ctx: VaultContext, permissions: List<String>): BooberVault {
        val vault = getVault(ctx)
        vault.findExistingPermissions(permissions)?.let {
            throw GoboException("Permission $it already exists for vault with vault name ${ctx.vaultName}.")
        }

        val permissionsSet = ((vault.permissions ?: emptyList()) + permissions).toSet()
        val updatedVault = vault.copy(permissions = permissionsSet.toList())
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun removeVaultPermissions(ctx: VaultContext, permissions: List<String>): BooberVault {
        val vault = getVault(ctx)

        vault.findMissingPermissions(permissions)?.let {
            throw GoboException("Permission $it does not exist on vault with vault name ${ctx.vaultName}.")
        }

        val updatedVault = vault.copy(permissions = vault.permissions?.minus(permissions))
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun addVaultSecrets(ctx: VaultContext, secrets: List<Secret>): BooberVault {
        val vault = getVault(ctx)
        vault.findExistingSecrets(secrets)?.let {
            throw GoboException("Secret $it already exists for vault with vault name ${ctx.vaultName}.")
        }

        val updatedVault = vault.copy(secrets = (vault.secrets ?: emptyMap()) + secrets.toBooberInput())
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun removeVaultSecrets(ctx: VaultContext, secretNames: List<String>): BooberVault {
        val vault = getVault(ctx)
        vault.findMissingSecrets(secretNames)?.let {
            throw GoboException("Secret $it does not exist on vault with vault name ${ctx.vaultName}.")
        }

        val updatedVault = vault.copy(secrets = vault.secrets?.minus(secretNames))
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun renameVaultSecret(ctx: VaultContext, secretName: String, newSecretName: String): BooberVault {
        val vault = getVault(ctx)
        vault.findMissingSecret(secretName)?.let {
            throw GoboException("The secret you try to rename from $secretName does not exist for the vault with name ${ctx.vaultName}.")
        }
        vault.findExistingSecret(newSecretName)?.let {
            throw GoboException("You can not rename $secretName to $newSecretName. The secret $newSecretName already exists.")
        }

        val updatedSecrets = vault.secrets?.get(secretName)?.let {
            vault.secrets.toMutableMap().apply {
                remove(secretName)
                put(newSecretName, it)
            }
        }
        val updatedVault = vault.copy(secrets = updatedSecrets)
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun updateVaultSecret(ctx: VaultContext, secretName: String, content: String): BooberVault {
        getVault(ctx).findMissingSecret(secretName)?.let {
            throw GoboException("Secret $it does not exist on vault with vault name ${ctx.vaultName}.")
        }

        booberWebClient.put<Map<String, String>>(
            url = "/v1/vault/${ctx.affiliationName}/${ctx.vaultName}/$secretName",
            body = mapOf("contents" to content),
            token = ctx.token
        ).response()
        return getVault(ctx)
    }

    private suspend fun putVault(token: String, affiliationName: String, input: BooberVaultInput) =
        booberWebClient.put<BooberVault>(
            url = "/v1/vault/$affiliationName",
            token = token,
            body = input
        ).response()

    private fun List<Secret>.toBooberInput() = this.map { it.name to it.base64Content }.toMap()

    private suspend fun checkIfVaultExists(ctx: VaultContext) {
        try {
            (getVault(ctx)).let { throw GoboException("Vault with vault name ${ctx.vaultName} already exists.") }
        } catch (e: BooberIntegrationException) {
            logger.debug { "Vault with name ${ctx.vaultName} does not exist. Vault will be created." }
        }
    }
}
