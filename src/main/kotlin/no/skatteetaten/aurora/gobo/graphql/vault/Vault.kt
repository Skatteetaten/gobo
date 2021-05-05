package no.skatteetaten.aurora.gobo.graphql.vault

import no.skatteetaten.aurora.gobo.integration.boober.BooberVault

data class Vault(
    val name: String,
    val hasAccess: Boolean,
    val permissions: List<String>? = null,
    val secrets: List<Secret>? = null
) {
    fun secrets(names: List<String>? = null) =
        if (names.isNullOrEmpty()) {
            secrets
        } else {
            secrets?.filter { names.contains(it.name) }
        }

    companion object {
        fun create(booberVault: BooberVault) = Vault(
            name = booberVault.name,
            hasAccess = booberVault.hasAccess,
            permissions = booberVault.permissions,
            secrets = booberVault.secrets?.map { Secret(it.key, it.value) }
        )
    }
}

data class Secret(val name: String, val base64Content: String)

data class CreateVaultInput(
    val affiliationName: String,
    val vaultName: String,
    val secrets: List<Secret>,
    val permissions: List<String>
)

data class DeleteVaultInput(val affiliationName: String, val vaultName: String)
data class DeleteVaultResponse(val affiliationName: String, val vaultName: String)

data class RenameVaultInput(
    val affiliationName: String,
    val vaultName: String,
    val newVaultName: String
)
