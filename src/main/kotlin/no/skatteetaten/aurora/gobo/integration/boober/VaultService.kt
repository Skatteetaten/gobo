package no.skatteetaten.aurora.gobo.integration.boober

import org.springframework.stereotype.Service
import no.skatteetaten.aurora.gobo.graphql.vault.CreateVaultInput

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

    suspend fun createVault(token: String, inputCreate: CreateVaultInput): BooberVault {
        return booberWebClient.put<BooberVault>(
            url = "/v1/vault/${inputCreate.affiliationName}",
            token = token,
            body = inputCreate.mapToPayload()
        ).response()
    }

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
        return booberWebClient.put<BooberVault>(
            url = "/v1/vault/$affiliationName",
            token = token,
            body = updatedVault.toInput()
        ).response()
    }

    suspend fun removeVaultPermissions(
        token: String,
        affiliationName: String,
        vaultName: String,
        permissions: List<String>
    ): BooberVault {
        val vault = getVault(affiliationName, vaultName, token)
        val updatedVault = vault.copy(permissions = vault.permissions?.minus(permissions))
        return booberWebClient.put<BooberVault>(
            url = "/v1/vault/$affiliationName",
            token = token,
            body = updatedVault.toInput()
        ).response()
    }
}
