package no.skatteetaten.aurora.gobo.integration.boober

import org.springframework.stereotype.Service
import no.skatteetaten.aurora.gobo.graphql.vault.CreateVaultInput
import no.skatteetaten.aurora.gobo.graphql.vault.Secret
import java.lang.IllegalArgumentException

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

    fun containsSecret(name: String) =
        secrets?.containsKey(name) ?: false

    fun doesNotContainSecret(name: String) = !containsSecret(name)
}

data class VaultContext(val token: String, val affiliationName: String, val vaultName: String)

@Service
class VaultService(private val booberWebClient: BooberWebClient) {

    suspend fun getVaults(token: String, affiliationName: String) =
        booberWebClient.get<BooberVault>(token = token, url = "/v1/vault/$affiliationName").responses()

    suspend fun getVault(ctx: VaultContext) =
        booberWebClient.get<BooberVault>(token = ctx.token, url = "/v1/vault/${ctx.affiliationName}/${ctx.vaultName}")
            .response()

    suspend fun createVault(token: String, input: CreateVaultInput) =
        putVault(token, input.affiliationName, input.mapToPayload())

    suspend fun deleteVault(ctx: VaultContext) {
        val url = "/v1/vault/${ctx.affiliationName}/${ctx.vaultName}"
        booberWebClient.delete<BooberVault>(
            url = url,
            token = ctx.token
        ).responses()
    }

    suspend fun addVaultPermissions(ctx: VaultContext, permissions: List<String>): BooberVault {
        val vault = getVault(ctx)
        val permissionsSet = ((vault.permissions ?: emptyList()) + permissions).toSet()
        val updatedVault = vault.copy(permissions = permissionsSet.toList())
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun removeVaultPermissions(ctx: VaultContext, permissions: List<String>): BooberVault {
        val vault = getVault(ctx)
        val updatedVault = vault.copy(permissions = vault.permissions?.minus(permissions))
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun addVaultSecrets(ctx: VaultContext, secrets: List<Secret>): BooberVault {
        val vault = getVault(ctx)
        secrets.forEach {
            if (vault.containsSecret(it.name)) {
                throw IllegalArgumentException("Secret named $it is already present in vault") // TODO validering
            }
        }

        val updatedVault = vault.copy(secrets = (vault.secrets ?: emptyMap()) + secrets.toBooberInput())
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun removeVaultSecrets(ctx: VaultContext, secretNames: List<String>): BooberVault {
        val vault = getVault(ctx)
        secretNames.forEach {
            if (vault.doesNotContainSecret(it)) {
                throw IllegalArgumentException("No secret named $it found in vault") // TODO validering
            }
        }

        val updatedVault = vault.copy(secrets = vault.secrets?.minus(secretNames))
        return putVault(ctx.token, ctx.affiliationName, updatedVault.toInput())
    }

    suspend fun renameVaultSecret(ctx: VaultContext, secretName: String, newSecretName: String): BooberVault {
        val vault = getVault(ctx)
        if (vault.doesNotContainSecret(secretName)) {
            throw IllegalArgumentException("No secret named $secretName found in vault") // TODO validering
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

    suspend fun updateVaultSecret(ctx: VaultContext, secretName: String, content: String): BooberVault {
        getVault(ctx).secrets?.get(secretName)
            ?: throw IllegalStateException("No secret with name $secretName found") // TODO validering

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
}
