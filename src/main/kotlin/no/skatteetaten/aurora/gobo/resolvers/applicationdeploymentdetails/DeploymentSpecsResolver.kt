package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import no.skatteetaten.aurora.gobo.resolvers.token
import org.dataloader.Try
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class DeploymentSpecsResolver(private val specLoader: NoCacheBatchDataLoader<UrlAndToken, Try<DeploymentSpec>>) :
    GraphQLResolver<DeploymentSpecs> {

    fun current(specs: DeploymentSpecs, dfe: DataFetchingEnvironment): CompletableFuture<Try<DeploymentSpec>>? =
        dfe.token?.let { token ->
            specs.deploymentSpecCurrent?.let { specLoader.load(UrlAndToken(it, token)) }
        }

    fun deployed(specs: DeploymentSpecs, dfe: DataFetchingEnvironment): CompletableFuture<Try<DeploymentSpec>>? =
        dfe.token?.let { token ->
            specs.deploymentSpecDeployed?.let { specLoader.load(UrlAndToken(it, token)) }
        }
}