package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.graphql.vault.Vault
import org.springframework.stereotype.Service
import no.skatteetaten.aurora.gobo.graphql.vault.VaultCreationInput

data class AuroraSecretVaultPayload(
    val name: String,
    val permissions: List<String>,
    val secrets: Map<String, String>?
)

@Service
class VaultService(private val booberWebClient: BooberWebClient) {

    suspend fun getVaults(affiliationName: String, token: String) =
        booberWebClient.get<Vault>(token = token, url = "/v1/vault/$affiliationName").responses()

    suspend fun getVault(affiliationName: String, vaultName: String, token: String) =
        booberWebClient.get<Vault>(token = token, url = "/v1/vault/$affiliationName/$vaultName").response()

    suspend fun createVault(token: String, input: VaultCreationInput): Vault {
        val url = "/v1/vault/${input.affiliationName}"
        return booberWebClient.put<Vault>(
            url = url,
            token = token,
            body = input.mapToPayload()
        ).response()
    }
}
