package no.skatteetaten.aurora.gobo.graphql.vault

import org.springframework.stereotype.Component
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.VaultService

@Component
class VaultListDataLoader(val vaultService: VaultService) : KeyDataLoader<String, List<Vault>> {
    override suspend fun getByKey(key: String, context: GoboGraphQLContext): List<Vault> {
        return vaultService.getVaults(affiliationName = key, token = context.token())
    }
}
