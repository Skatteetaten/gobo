package no.skatteetaten.aurora.gobo.graphql.webseal

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.springframework.stereotype.Component

@Component
class WebsealStateDataLoader(private val websealAffiliationService: WebsealAffiliationService) : GoboDataLoader<String, List<WebsealState>>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GraphQLContext): Map<String, List<WebsealState>> {
        return websealAffiliationService
            .getWebsealState(keys.toList())
            .mapValues { states ->
                states.value.map { state -> WebsealState.create(state) }
            }
    }
}
