package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.resolvers.MultipleKeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class AffiliationDataLoader(private val websealAffiliationService: WebsealAffiliationService) :
    MultipleKeysDataLoader<String, List<WebsealState>> {

    override fun getByKeys(user: User, keys: MutableSet<String>): Map<String, Try<List<WebsealState>>> {
        val websealStates = websealAffiliationService.getWebsealState(keys.toList())
        return websealStates.mapValues {
            val state = it.value.map { resource -> WebsealState.create(resource) }
            Try.succeeded(state)
        }
    }
}
