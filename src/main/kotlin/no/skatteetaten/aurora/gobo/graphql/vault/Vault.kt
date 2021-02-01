package no.skatteetaten.aurora.gobo.graphql.vault

import com.expediagroup.graphql.annotations.GraphQLIgnore
import no.skatteetaten.aurora.gobo.integration.boober.AuroraSecretVaultPayload

data class Secret(val key: String, val value: String)

data class Vault(
    val name: String,
    val hasAccess: Boolean,
    val permissions: List<String>?,

    @GraphQLIgnore
    val secrets: Map<String, String>?
) {
    fun secrets() = secrets?.map { Secret(it.key, it.value) }
}

data class VaultCreationInput(
    val affiliationName: String,
    val vaultName: String,
    val secrets: List<Secret>,
    val permissions: List<String>
) {
    @GraphQLIgnore
    fun mapToPayload() = AuroraSecretVaultPayload(vaultName, permissions, secrets.associate { it.key to it.value })
}
