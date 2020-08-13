package no.skatteetaten.aurora.gobo.resolvers.webseal

import no.skatteetaten.aurora.gobo.MultipleKeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class WebsealStateListDataLoader(private val websealAffiliationService: WebsealAffiliationService) :
    MultipleKeysDataLoader<String, List<WebsealState>> {

    override suspend fun getByKeys(keys: Set<String>, ctx: GoboGraphQLContext): Map<String, Try<List<WebsealState>>> {
        val websealStates = websealAffiliationService.getWebsealState(keys.toList())
        return websealStates.mapValues {
            val state = it.value.map { resource -> WebsealState.create(resource) }
            Try.succeeded(state)
        }
    }
}
