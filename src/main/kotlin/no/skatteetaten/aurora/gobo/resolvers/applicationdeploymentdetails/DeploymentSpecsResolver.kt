package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.resolvers.loader
import org.dataloader.Try
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class DeploymentSpecsResolver : GraphQLResolver<DeploymentSpecs> {

    fun current(specs: DeploymentSpecs, dfe: DataFetchingEnvironment): CompletableFuture<Try<DeploymentSpec>>? =
        specs.deploymentSpecCurrent?.let { dfe.loader(DeploymentSpec::class).load(it) }

    fun deployed(specs: DeploymentSpecs, dfe: DataFetchingEnvironment): CompletableFuture<Try<DeploymentSpec>>? =
        specs.deploymentSpecDeployed?.let { dfe.loader(DeploymentSpec::class).load(it) }
}