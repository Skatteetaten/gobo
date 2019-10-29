package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.coxautodev.graphql.tools.GraphQLResolver
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetchingEnvironment
import java.net.URL
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.resolvers.KeyDataLoader
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.resolvers.user.User
import org.dataloader.Try
import org.springframework.stereotype.Component
import reactor.core.publisher.toMono

class DeploymentSpecs(
    val deploymentSpecCurrent: URL?,
    val deploymentSpecDeployed: URL?
)

data class DeploymentSpec(val jsonRepresentation: String)

@Component
class DeploymentSpecsResolver : GraphQLResolver<DeploymentSpecs> {

    fun current(specs: DeploymentSpecs, dfe: DataFetchingEnvironment) =
        specs.deploymentSpecCurrent?.let { dfe.loader(DeploymentSpecDataLoader::class).load(it) }

    fun deployed(specs: DeploymentSpecs, dfe: DataFetchingEnvironment) =
        specs.deploymentSpecDeployed?.let { dfe.loader(DeploymentSpecDataLoader::class).load(it) }
}

@Component
class DeploymentSpecDataLoader(
    private val booberWebClient: BooberWebClient,
    private val objectMapper: ObjectMapper
) : KeyDataLoader<URL, DeploymentSpec> {
    override fun getByKey(user: User, key: URL): Try<DeploymentSpec> {
        return Try.tryCall {
            booberWebClient.get<JsonNode>(user.token, key.toString())
                .map { DeploymentSpec(objectMapper.writeValueAsString(it)) }
                .toMono()
                .blockNonNullAndHandleError()
        }
    }
}
