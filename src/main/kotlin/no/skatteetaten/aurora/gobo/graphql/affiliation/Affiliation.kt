package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.annotations.GraphQLDescription
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.GoboEdge
import no.skatteetaten.aurora.gobo.graphql.application.Application
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfig
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfigKey
import no.skatteetaten.aurora.gobo.graphql.database.DatabaseSchema
import no.skatteetaten.aurora.gobo.graphql.errorhandling.GraphQLExceptionWrapper
import no.skatteetaten.aurora.gobo.graphql.load
import no.skatteetaten.aurora.gobo.graphql.loadBatchList
import no.skatteetaten.aurora.gobo.graphql.loadMany
import no.skatteetaten.aurora.gobo.graphql.loadOrThrow
import no.skatteetaten.aurora.gobo.graphql.vault.Vault
import no.skatteetaten.aurora.gobo.graphql.vault.VaultKey
import no.skatteetaten.aurora.gobo.graphql.webseal.WebsealState
import no.skatteetaten.aurora.gobo.security.checkValidUserToken

data class Affiliation(val name: String) {

    suspend fun auroraConfig(refInput: String?, dfe: DataFetchingEnvironment): DataFetcherResult<AuroraConfig?> {
        dfe.checkValidUserToken()
        return dfe.load(AuroraConfigKey(name = name, refInput = refInput ?: "master"))
    }

    @GraphQLDescription("Get all database schemas for the given affiliation")
    suspend fun databaseSchemas(dfe: DataFetchingEnvironment): List<DatabaseSchema> = dfe.loadMany(name)

    suspend fun websealStates(dfe: DataFetchingEnvironment): List<WebsealState> = dfe.loadMany(name)
    suspend fun vaults(names: List<String>?, dfe: DataFetchingEnvironment): DataFetcherResult<List<Vault>> {
        dfe.checkValidUserToken()
        return if (names.isNullOrEmpty()) {
            DataFetcherResult.newResult<List<Vault>>().data(dfe.loadMany(name)).build()
        } else {
            val values = names.map {
                kotlin.runCatching {
                    dfe.loadOrThrow<VaultKey, Vault>(VaultKey(name, it))
                }.recoverCatching {
                    it
                }.getOrThrow()
            }.groupBy {
                when (it) {
                    is Throwable -> "failure"
                    else -> "success"
                }
            }

            val failure = values["failure"]?.let { it as List<Throwable> } ?: emptyList()
            val success = values["success"]?.let { it as List<Vault> } ?: emptyList()

            DataFetcherResult.newResult<List<Vault>>().data(success).errors(failure.map { GraphQLExceptionWrapper(it) })
                .build()
        }
    }

    fun applications(dfe: DataFetchingEnvironment) = dfe.loadBatchList<String, Application>(name)
}

data class AffiliationEdge(
    val node: Affiliation
) : GoboEdge(node.name)

data class AffiliationsConnection(
    val edges: List<AffiliationEdge>,
    val totalCount: Int = edges.size
)
