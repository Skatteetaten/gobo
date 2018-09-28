package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoaderFlux
import org.dataloader.Try
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentDetailsQueryResolver(
    private val detailsLoader: NoCacheBatchDataLoaderFlux<String, Try<ApplicationDeploymentDetails>>
) : GraphQLQueryResolver {

    fun applicationDeploymentDetails(id: String) = detailsLoader.load(id)
}