package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.GoboEdge
import no.skatteetaten.aurora.gobo.graphql.application.Application
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfig
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfigKey
import no.skatteetaten.aurora.gobo.graphql.database.DatabaseSchema
import no.skatteetaten.aurora.gobo.graphql.load
import no.skatteetaten.aurora.gobo.graphql.loadListValue
import no.skatteetaten.aurora.gobo.graphql.loadMany
import no.skatteetaten.aurora.gobo.graphql.loadOrThrow
import no.skatteetaten.aurora.gobo.graphql.newDataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.vault.Vault
import no.skatteetaten.aurora.gobo.graphql.vault.VaultKey
import no.skatteetaten.aurora.gobo.graphql.webseal.WebsealState
import no.skatteetaten.aurora.gobo.security.checkValidUserToken

data class Affiliation(val name: String) {

    suspend fun auroraConfig(refInput: String? = null, dfe: DataFetchingEnvironment): DataFetcherResult<AuroraConfig?> {
        dfe.checkValidUserToken()
        return dfe.load(AuroraConfigKey(name = name, refInput = refInput ?: "master"))
    }

    @GraphQLDescription("Get all database schemas for the given affiliation")
    suspend fun databaseSchemas(dfe: DataFetchingEnvironment): List<DatabaseSchema> = dfe.loadMany(name)

    suspend fun websealStates(dfe: DataFetchingEnvironment): List<WebsealState> = dfe.loadMany(name)
    suspend fun vaults(names: List<String>? = null, dfe: DataFetchingEnvironment): DataFetcherResult<List<Vault>> {
        dfe.checkValidUserToken()
        return if (names.isNullOrEmpty()) {
            newDataFetcherResult(dfe.loadMany(name))
        } else {
            val values = names.map {
                runCatching { dfe.loadOrThrow<VaultKey, Vault>(VaultKey(name, it)) }
                    .recoverCatching { it }.getOrThrow()
            }

            newDataFetcherResult(values.successes(), values.failures())
        }
    }

    private fun List<Any>.successes() = this.filterIsInstance<Vault>()
    private fun List<Any>.failures() = this.filterIsInstance<Throwable>()

    fun applications(dfe: DataFetchingEnvironment) = dfe.loadListValue<String, Application>(name)
}

data class AffiliationEdge(
    val node: Affiliation
) : GoboEdge(node.name)

data class AffiliationsConnection(
    val edges: List<AffiliationEdge>,
    val totalCount: Int = edges.size
)
