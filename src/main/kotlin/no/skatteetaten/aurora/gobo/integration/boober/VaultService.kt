package no.skatteetaten.aurora.gobo.integration.boober

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.graphql.vault.CreateVaultInput
import no.skatteetaten.aurora.gobo.graphql.vault.Secret

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
    fun toInput() = BooberVaultInput(name = name, permissions = permissions, secrets = secrets)
}

data class VaultContext(val token: String, val affiliationName: String, val vaultName: String)

private val logger = KotlinLogging.logger { }

@Service
class VaultService(private val booberWebClient: BooberWebClient) {

    suspend fun getVaults(token: String, affiliationName: String) =
        booberWebClient.get<BooberVault>(token = token, url = "/v1/vault/$affiliationName").responses()

    suspend fun getVault(ctx: VaultContext) =
        booberWebClient.get<BooberVault>(token = ctx.token, url = "/v1/vault/${ctx.affiliationName}/${ctx.vaultName}")
            .response()

    suspend fun createVault(token: String, input: CreateVaultInput): BooberVault {
        checkIfVaultExists(affiliationName = input.affiliationName, vaultName = input.vaultName, token)
        return putVault(token, input.affiliationName, input.mapToPayload())
    }

    suspend fun deleteVault(ctx: VaultContext) {
        val vault = getVault(ctx)
        val url = "/v1/vault/${ctx.affiliationName}/${ctx.vaultName}"
        booberWebClient.delete<BooberVault>(
            url = url,
            token = ctx.token
        ).responses()
    }

    suspend fun addVaultPermissions(ctx: VaultContext, permissions: List<String>): BooberVault {
        val vault = getVault(ctx)
        permissions.forEach {
            if (vault.permissions?.contains(it) == true) {
                throw GoboException("Permission $it already exists for vault with vault name ${ctx.vaultName}.")
            }
        }
        val permissionsSet = ((vault.permissions ?: emptyList()) + permissions).toSet()
        val updatedVault = vault.copy(permissions = permissionsSet.toList())
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun removeVaultPermissions(ctx: VaultContext, permissions: List<String>): BooberVault {
        val vault = getVault(ctx)
        permissions.forEach {
            if (vault.permissions?.contains(it) == false) {
                throw GoboException("Permission $it does not exist on vault with vault name ${ctx.vaultName}.")
            }
        }
        val updatedVault = vault.copy(permissions = vault.permissions?.minus(permissions))
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun addVaultSecrets(ctx: VaultContext, secrets: List<Secret>): BooberVault {
        val vault = getVault(ctx)
        secrets.forEach {
            if (vault.secrets?.contains(it.name) == true) {
                throw GoboException("Secret ${it.name} already exists for vault with vault name ${ctx.vaultName}.")
            }
        }
        val updatedVault = vault.copy(secrets = (vault.secrets ?: emptyMap()) + secrets.toBooberInput())
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun removeVaultSecrets(ctx: VaultContext, secrets: List<Secret>): BooberVault {
        val vault = getVault(ctx)
        secrets.forEach {
            if (vault.secrets?.contains(it.name) == false) {
                throw GoboException("Secret ${it.name} does not exist on vault with vault name ${ctx.vaultName}.")
            }
        }
        val updatedVault = vault.copy(secrets = vault.secrets?.minus(secrets.map { it.name }))
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun renameVaultSecret(ctx: VaultContext, secretName: String, newSecretName: String): BooberVault {
        val vault = getVault(ctx)
        if (vault.secrets?.contains(secretName) == false) {
            throw GoboException("The secret you try to rename from $secretName does not exist for the vault with name ${ctx.vaultName}.")
        }
        if (vault.secrets?.contains(newSecretName) == true) {
            throw GoboException("The secret you try to rename to $secretName already exists for the vault with name ${ctx.vaultName}.")
        }
        val updatedSecrets = vault.secrets?.get(secretName)?.let {
            vault.secrets.toMutableMap().apply {
                remove(secretName)
                put(newSecretName, it)
            }
        } ?: throw IllegalStateException("No secret with name $secretName found") // TODO validering
        val updatedVault = vault.copy(secrets = updatedSecrets)
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    private suspend fun putVault(token: String, affiliationName: String, input: BooberVaultInput) =
        booberWebClient.put<BooberVault>(
            url = "/v1/vault/$affiliationName",
            token = token,
            body = input
        ).response()

    private fun List<Secret>.toBooberInput() = this.map { it.name to it.base64Content }.toMap()

    private suspend fun checkIfVaultExists(affiliationName: String, vaultName: String, token: String) {
        try {
            (getVault(VaultContext(token = token, affiliationName = affiliationName, vaultName = vaultName))).name.equals(vaultName)
                .let { throw GoboException("Vault with vault name $vaultName already exists.") }
        } catch (e: WebClientResponseException) {
            logger.debug { "Vault with name $vaultName does not exist. Vault will be created." }
        }
    }
}
