package no.skatteetaten.aurora.gobo.integration.boober

import org.springframework.stereotype.Service
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

@Service
class VaultService(private val booberWebClient: BooberWebClient) {

    suspend fun getVaults(affiliationName: String, token: String) =
        booberWebClient.get<BooberVault>(token = token, url = "/v1/vault/$affiliationName").responses()

    suspend fun getVault(affiliationName: String, vaultName: String, token: String) =
        booberWebClient.get<BooberVault>(token = token, url = "/v1/vault/$affiliationName/$vaultName").response()

    suspend fun createVault(token: String, input: CreateVaultInput) =
        putVault(token, input.affiliationName, input.mapToPayload())

    suspend fun deleteVault(
        token: String,
        affiliationName: String,
        vaultName: String
    ) {
        val url = "/v1/vault/$affiliationName/$vaultName"
        booberWebClient.delete<BooberVault>(
            url = url,
            token = token
        ).responses()
    }

    suspend fun addVaultPermissions(
        token: String,
        affiliationName: String,
        vaultName: String,
        permissions: List<String>
    ): BooberVault {
        val vault = getVault(affiliationName, vaultName, token)
        val permissionsSet = ((vault.permissions ?: emptyList()) + permissions).toSet()
        val updatedVault = vault.copy(permissions = permissionsSet.toList())
        return putVault(token, affiliationName, updatedVault.toInput())
    }

    suspend fun removeVaultPermissions(
        token: String,
        affiliationName: String,
        vaultName: String,
        permissions: List<String>
    ): BooberVault {
        val vault = getVault(affiliationName, vaultName, token)
        val updatedVault = vault.copy(permissions = vault.permissions?.minus(permissions))
        return putVault(token, affiliationName, updatedVault.toInput())
    }

    suspend fun addVaultSecrets(
        token: String,
        affiliationName: String,
        vaultName: String,
        secrets: List<Secret>
    ): BooberVault {
        val vault = getVault(affiliationName, vaultName, token)
        val updatedVault = vault.copy(secrets = (vault.secrets ?: emptyMap()) + secrets.toBooberInput())
        return putVault(token, affiliationName, updatedVault.toInput())
    }

    suspend fun removeVaultSecrets(
        token: String,
        affiliationName: String,
        vaultName: String,
        secrets: List<Secret>
    ): BooberVault {
        val vault = getVault(affiliationName, vaultName, token)
        val updatedVault = vault.copy(secrets = vault.secrets?.minus(secrets.map { it.name }))
        return putVault(token, affiliationName, updatedVault.toInput())
    }

    private suspend fun putVault(token: String, affiliationName: String, input: BooberVaultInput) =
        booberWebClient.put<BooberVault>(
            url = "/v1/vault/$affiliationName",
            token = token,
            body = input
        ).response()

    private fun List<Secret>.toBooberInput() = this.map { it.name to it.base64Content }.toMap()
}
