package no.skatteetaten.aurora.gobo.graphql.vault

data class AddVaultPermissionsInput(
    val affiliationName: String,
    val vaultName: String,
    val permissions: List<String>
)

data class RemoveVaultPermissionsInput(
    val affiliationName: String,
    val vaultName: String,
    val permissions: List<String>
)
