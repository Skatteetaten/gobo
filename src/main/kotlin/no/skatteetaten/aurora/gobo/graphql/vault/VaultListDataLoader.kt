package no.skatteetaten.aurora.gobo.graphql.vault

import org.springframework.stereotype.Component
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.integration.boober.responses

@Component
class VaultListDataLoader(val booberWebClient: BooberWebClient) : KeyDataLoader<String, List<Vault>> {
    override suspend fun getByKey(key: String, context: GoboGraphQLContext): List<Vault> {
        return booberWebClient.get<Vault>(token = context.token(), url = "/v1/vault/$key").responses()
    }
}
