package no.skatteetaten.aurora.gobo.resolvers.namespace

import no.skatteetaten.aurora.gobo.resolvers.KeysDataLoader
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.springframework.stereotype.Component

@Component
class NamespaceDataLoader : KeysDataLoader<ApplicationDeployment, Namespace> {
    override fun getByKeys(user: User, keys: List<ApplicationDeployment>): List<Namespace> =
        keys.map {
            Namespace(it.namespaceId, it.affiliationId)
        }
}