package no.skatteetaten.aurora.gobo.graphql.vault

data class AddVaultSecretsInput(val affiliationName: String, val vaultName: String, val secrets: List<Secret>)

data class RemoveVaultSecretsInput(val affiliationName: String, val vaultName: String, val secrets: List<Secret>)

data class RenameVaultSecretInput(
    val affiliationName: String,
    val vaultName: String,
    val secretName: String,
    val newSecretName: String
)
