package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoaderFlux
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails.ApplicationDeploymentDetails
import no.skatteetaten.aurora.gobo.resolvers.namespace.Namespace
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentResolver(
    private val detailsLoader: NoCacheBatchDataLoaderFlux<String, Try<ApplicationDeploymentDetails>>
) : GraphQLResolver<ApplicationDeployment> {

    fun affiliation(applicationDeployment: ApplicationDeployment): Affiliation =
        Affiliation(applicationDeployment.affiliationId)

    fun namespace(applicationDeployment: ApplicationDeployment): Namespace =
        Namespace(applicationDeployment.namespaceId, applicationDeployment.affiliationId)

    fun details(applicationDeployment: ApplicationDeployment) = detailsLoader.load(applicationDeployment.id)
}