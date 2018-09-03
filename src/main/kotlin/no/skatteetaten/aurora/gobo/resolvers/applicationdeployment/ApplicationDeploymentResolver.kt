package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.namespace.Namespace
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentResolver : GraphQLResolver<ApplicationDeployment> {

    fun affiliation(applicationDeployment: ApplicationDeployment): Affiliation =
        Affiliation(applicationDeployment.affiliationId)

    fun namespace(applicationDeployment: ApplicationDeployment): Namespace =
        Namespace(applicationDeployment.namespaceId, applicationDeployment.affiliationId)
}