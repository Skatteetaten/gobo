package no.skatteetaten.aurora.gobo.graphql.webseal

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.springframework.stereotype.Component

@Component
class WebsealStateListDataLoader(private val websealAffiliationService: WebsealAffiliationService) :
    KeyDataLoader<String, List<WebsealState>> {

    override suspend fun getByKey(key: String, context: GoboGraphQLContext): List<WebsealState> {
        val websealStates = websealAffiliationService.getWebsealState(listOf(key))
        return websealStates[key]?.map { WebsealState.create(it) } ?: emptyList()
    }
}
