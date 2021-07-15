package no.skatteetaten.aurora.gobo.graphql.webseal

import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.springframework.stereotype.Component

@Component
class WebsealStateDataLoader(private val websealAffiliationService: WebsealAffiliationService) : GoboDataLoader<String, List<WebsealState>>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GoboGraphQLContext): Map<String, List<WebsealState>> {
        return websealAffiliationService
            .getWebsealState(keys.toList())
            .mapValues { states ->
                states.value.map { state -> WebsealState.create(state) }
            }
    }
}
