package no.skatteetaten.aurora.gobo.graphql.vault

import org.springframework.stereotype.Component
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.integration.boober.response

data class VaultKey(
    val affiliationName: String,
    val vaultName: String

)

@Component
class VaultDataLoader(val booberWebClient: BooberWebClient) : KeyDataLoader<VaultKey, Vault> {
    override suspend fun getByKey(key: VaultKey, context: GoboGraphQLContext): Vault {
        return booberWebClient.get<Vault>(token = context.token(), url = "/v1/vault/${key.affiliationName}/${key.vaultName}").response()
    }
}
