package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.annotations.GraphQLDescription
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.GoboEdge
import no.skatteetaten.aurora.gobo.graphql.GoboItems
import no.skatteetaten.aurora.gobo.graphql.database.DatabaseSchema
import no.skatteetaten.aurora.gobo.graphql.loadMany
import no.skatteetaten.aurora.gobo.graphql.loadOrThrow
import no.skatteetaten.aurora.gobo.graphql.vault.Vault
import no.skatteetaten.aurora.gobo.graphql.vault.VaultKey
import no.skatteetaten.aurora.gobo.graphql.webseal.WebsealState

data class Affiliation(val name: String) {

    @GraphQLDescription("Get all database schemas for the given affiliation")
    suspend fun databaseSchemas(dfe: DataFetchingEnvironment): List<DatabaseSchema> = dfe.loadMany(name)

    suspend fun websealStates(dfe: DataFetchingEnvironment): List<WebsealState> = dfe.loadMany(name)
    suspend fun vaults(names: List<String>?, dfe: DataFetchingEnvironment): List<Vault> {
        return if (names.isNullOrEmpty()) {
            dfe.loadMany(name)
        } else {
            names.map {
                dfe.loadOrThrow(VaultKey(name, it)) // TODO: check boober result, should we return partial result
            }
        }
    }
}

data class AffiliationEdge(
    @Deprecated(message = "edges.node is deprecated", replaceWith = ReplaceWith("items"))
    val node: Affiliation
) : GoboEdge(node.name)

data class AffiliationsConnection(
    @Deprecated(message = "edges.node is deprecated", replaceWith = ReplaceWith("items"))
    val edges: List<AffiliationEdge>,
    val items: List<Affiliation> = edges.map { it.node }
) : GoboItems(items)
