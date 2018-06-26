package no.skatteetaten.aurora.gobo.resolvers.applicationinstance

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.namespace.Namespace
import org.springframework.stereotype.Component

@Component
class ApplicationInstanceResolver : GraphQLResolver<ApplicationInstance> {

    fun affiliation(applicationInstance: ApplicationInstance): Affiliation =
        Affiliation(applicationInstance.affiliationId)

    fun namespace(applicationInstance: ApplicationInstance): Namespace =
        Namespace(applicationInstance.namespaceId, applicationInstance.affiliationId)
}