package no.skatteetaten.aurora.gobo.graphql.vault

import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.GoboDataLoader
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
class VaultBatchDataLoader(private val vaultService: VaultService) :
    GoboDataLoader<VaultKey, DataFetcherResult<List<Vault>>>() {
    override suspend fun getByKeys(
        keys: Set<VaultKey>,
        ctx: GoboGraphQLContext
    ): Map<VaultKey, DataFetcherResult<List<Vault>>> {
        return keys.associateWith { vaultKey ->
            when {
                vaultKey.vaultNames.isNullOrEmpty() -> newDataFetcherResult(getAllVaults(ctx, vaultKey))
                else -> {
                    val results = getNamedVaults(vaultKey.vaultNames, ctx, vaultKey)
                    val vaults = results.filterIsInstance<Vault>()
                    val errors = results.filterIsInstance<Throwable>()
                    newDataFetcherResult(vaults, errors)
                }
            }
        }
    }

    private suspend fun getNamedVaults(
        vaultNames: List<String>,
        ctx: GoboGraphQLContext,
        vaultKey: VaultKey
    ) = vaultNames.map {
        runCatching {
            vaultService.getVault(VaultContext(ctx.token(), vaultKey.affiliationName, it)).let {
                Vault.create(it)
            }
        }.recoverCatching {
            it
        }.getOrThrow()
    }

    private suspend fun getAllVaults(
        ctx: GoboGraphQLContext,
        vaultKey: VaultKey
    ) =
        vaultService
            .getVaults(ctx.token(), vaultKey.affiliationName)
            .map { Vault.create(it) }
}
