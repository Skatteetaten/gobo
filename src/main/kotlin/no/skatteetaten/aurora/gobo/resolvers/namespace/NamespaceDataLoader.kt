package no.skatteetaten.aurora.gobo.resolvers.namespace

import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.ApplicationInstance
import org.springframework.stereotype.Component

@Component
class NamespaceDataLoader(
) : KeysDataLoader<ApplicationInstance, Namespace> {
    override fun getByKeys(keys: List<ApplicationInstance>): List<Namespace> =
        keys.map {
            Namespace(it.namespaceId, it.affiliationId)
        }
}