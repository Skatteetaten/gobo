package no.skatteetaten.aurora.gobo.graphql.vault

import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.newDataFetcherResult
import no.skatteetaten.aurora.gobo.integration.boober.VaultContext
import no.skatteetaten.aurora.gobo.integration.boober.VaultService
import org.springframework.stereotype.Component

data class VaultKey(
    val affiliationName: String,
    val vaultNames: List<String>?
)

@Component
class VaultDataLoader(private val vaultService: VaultService) :
    GoboDataLoader<VaultKey, DataFetcherResult<List<Vault>>>() {
    override suspend fun getByKeys(
        keys: Set<VaultKey>,
        ctx: GoboGraphQLContext
    ): Map<VaultKey, DataFetcherResult<List<Vault>>> {
        return keys.associateWith { vaultKey ->
            when {
                vaultKey.vaultNames.isNullOrEmpty() -> newDataFetcherResult(getAllVaults(ctx.token(), vaultKey.affiliationName))
                else -> {
                    val results = getNamedVaults(ctx.token(), vaultKey.affiliationName, vaultKey.vaultNames)
                    val vaults = results.filterIsInstance<Vault>()
                    val errors = results.filterIsInstance<Throwable>()
                    newDataFetcherResult(vaults, errors)
                }
            }
        }
    }

    private suspend fun getAllVaults(
        token: String,
        affiliation: String
    ) =
        vaultService
            .getVaults(token, affiliation)
            .map { Vault.create(it) }

    private suspend fun getNamedVaults(
        token: String,
        affiliation: String,
        vaultNames: List<String>
    ) = vaultNames.map {
        runCatching {
            vaultService.getVault(VaultContext(token, affiliation, it))
                .let { Vault.create(it) }
        }.recoverCatching {
            it
        }.getOrThrow()
    }
}
