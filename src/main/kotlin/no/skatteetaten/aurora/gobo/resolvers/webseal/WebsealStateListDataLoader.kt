package no.skatteetaten.aurora.gobo.resolvers.webseal

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.MultipleKeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.dataloader.Try
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class WebsealStateListDataLoader(private val websealAffiliationService: WebsealAffiliationService) :
    MultipleKeysDataLoader<String, List<WebsealState>> {

    override suspend fun getByKeys(keys: Set<String>, ctx: GoboGraphQLContext): Map<String, Try<List<WebsealState>>> {
        logger.info("webseal data loader!, $keys")
        val websealStates = websealAffiliationService.getWebsealState(keys.toList())
        return websealStates.mapValues {
            val state = it.value.map { resource -> WebsealState.create(resource) }
            Try.succeeded(state)
        }
    }
}
