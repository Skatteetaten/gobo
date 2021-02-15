package no.skatteetaten.aurora.gobo.graphql.vault

import org.springframework.stereotype.Component
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.VaultContext
import no.skatteetaten.aurora.gobo.integration.boober.VaultService

data class VaultKey(
    val affiliationName: String,
    val vaultName: String
)

@Component
class VaultDataLoader(val vaultService: VaultService) : KeyDataLoader<VaultKey, Vault> {
    override suspend fun getByKey(key: VaultKey, context: GoboGraphQLContext): Vault {
        val vault = vaultService.getVault(
            VaultContext(
                token = context.token(),
                affiliationName = key.affiliationName,
                vaultName = key.vaultName
            )
        )
        return Vault.create(vault)
    }
}
