package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.resolvers.NoCacheBatchDataLoader
import org.dataloader.Try
import org.springframework.stereotype.Component
import java.net.URL
import java.util.concurrent.CompletableFuture

@Component
class DeploymentSpecsResolver(private val specLoader: NoCacheBatchDataLoader<URL, Try<DeploymentSpec>>) : GraphQLResolver<DeploymentSpecs> {

    fun current(deploymentSpecs: DeploymentSpecs): CompletableFuture<Try<DeploymentSpec>>? {
        return deploymentSpecs.deploymentSpecCurrent?.let { specLoader.load(it) }
    }

    fun deployed(deploymentSpecs: DeploymentSpecs): CompletableFuture<Try<DeploymentSpec>>? {
        return deploymentSpecs.deploymentSpecDeployed?.let { specLoader.load(it) }
    }
}