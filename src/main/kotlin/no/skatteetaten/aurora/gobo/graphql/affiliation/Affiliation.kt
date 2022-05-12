package no.skatteetaten.aurora.gobo.graphql.affiliation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.graphql.GoboEdge
import no.skatteetaten.aurora.gobo.graphql.application.Application
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfig
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfigKey
import no.skatteetaten.aurora.gobo.graphql.database.DatabaseSchema
import no.skatteetaten.aurora.gobo.graphql.loadValue
import no.skatteetaten.aurora.gobo.graphql.vault.Vault
import no.skatteetaten.aurora.gobo.graphql.vault.VaultDataLoader
import no.skatteetaten.aurora.gobo.graphql.vault.VaultKey
import no.skatteetaten.aurora.gobo.graphql.webseal.WebsealState
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import java.util.concurrent.CompletableFuture
import no.skatteetaten.aurora.gobo.graphql.storagegrid.StorageGrid

data class Affiliation(val name: String) {

    fun auroraConfig(refInput: String? = null, dfe: DataFetchingEnvironment): CompletableFuture<AuroraConfig> {
        runBlocking { dfe.checkValidUserToken() } // TODO @PreAuthorize?
        return dfe.loadValue(AuroraConfigKey(name = name, refInput = refInput ?: "master"))
    }

    @GraphQLDescription("Get all database schemas for the given affiliation")
    fun databaseSchemas(dfe: DataFetchingEnvironment) = dfe.loadValue<String, List<DatabaseSchema>>(name)

    fun storageGrid(dfe: DataFetchingEnvironment) = StorageGrid(name)

    fun websealStates(dfe: DataFetchingEnvironment) = dfe.loadValue<String, List<WebsealState>>(name)

    fun vaults(
        names: List<String>? = null,
        dfe: DataFetchingEnvironment
    ): CompletableFuture<DataFetcherResult<List<Vault>>> {
        runBlocking { dfe.checkValidUserToken() } // TODO @PreAuthorize?
        return dfe.loadValue(
            key = VaultKey(affiliationName = name, vaultNames = names),
            loaderClass = VaultDataLoader::class
        )
    }

    fun applications(dfe: DataFetchingEnvironment) = dfe.loadValue<String, List<Application>>(name)
}

data class AffiliationEdge(
    val node: Affiliation
) : GoboEdge(node.name)

data class AffiliationsConnection(
    val edges: List<AffiliationEdge>,
    val totalCount: Int = edges.size
)
