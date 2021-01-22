package no.skatteetaten.aurora.gobo.graphql.vault

import com.expediagroup.graphql.annotations.GraphQLIgnore
import no.skatteetaten.aurora.gobo.graphql.database.JdbcUser

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
    val vaultName: String,
    val filePathval: String,
    val groups: List<String>
)
