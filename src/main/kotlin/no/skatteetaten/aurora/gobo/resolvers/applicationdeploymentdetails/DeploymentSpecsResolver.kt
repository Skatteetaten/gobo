package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.security.UserService
import org.dataloader.Try
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class DeploymentSpecsResolver(private val userService: UserService) : GraphQLResolver<DeploymentSpecs> {

    fun current(specs: DeploymentSpecs, dfe: DataFetchingEnvironment): CompletableFuture<Try<DeploymentSpec>>? =
        userService.getToken().let { token ->
            specs.deploymentSpecCurrent?.let { dfe.loader(DeploymentSpec::class).load(UrlAndToken(it, token)) }
        }

    fun deployed(specs: DeploymentSpecs, dfe: DataFetchingEnvironment): CompletableFuture<Try<DeploymentSpec>>? =
        userService.getToken().let { token ->
            specs.deploymentSpecDeployed?.let { dfe.loader(DeploymentSpec::class).load(UrlAndToken(it, token)) }
        }
}