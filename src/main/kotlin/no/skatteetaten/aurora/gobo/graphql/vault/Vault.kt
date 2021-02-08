package no.skatteetaten.aurora.gobo.graphql.vault

import com.expediagroup.graphql.annotations.GraphQLIgnore
import no.skatteetaten.aurora.gobo.integration.boober.BooberVault
import no.skatteetaten.aurora.gobo.integration.boober.BooberVaultInput

data class Vault(
    val name: String,
    val hasAccess: Boolean,
    val permissions: List<String>?,
    val secrets: List<Secret>?
) {
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
) {
    @GraphQLIgnore
    fun toBooberInput() = BooberVaultInput(vaultName, permissions, secrets.map { it.name to it.base64Content }.toMap())
}

data class DeleteVaultInput(val affiliationName: String, val vaultName: String)
data class DeleteVaultResponse(val affiliationName: String, val vaultName: String)
