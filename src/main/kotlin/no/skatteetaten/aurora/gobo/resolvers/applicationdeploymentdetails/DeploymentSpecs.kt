package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import com.expediagroup.graphql.spring.operations.Query
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.MyGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.load
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.stereotype.Component
import reactor.kotlin.core.publisher.toMono
import java.net.URL

class DeploymentSpecs(
    val deploymentSpecCurrent: URL?,
    val deploymentSpecDeployed: URL?
)

data class DeploymentSpec(val jsonRepresentation: String)

@Component
class DeploymentSpecsResolver : Query {

    suspend fun current(specs: DeploymentSpecs, dfe: DataFetchingEnvironment) =
        specs.deploymentSpecCurrent?.let { dfe.load<URL, DeploymentSpec>(it) }

    suspend fun deployed(specs: DeploymentSpecs, dfe: DataFetchingEnvironment) =
        specs.deploymentSpecDeployed?.let { dfe.load<URL, DeploymentSpec>(it) }
}

@Component
class DeploymentSpecDataLoader(
    private val booberWebClient: BooberWebClient,
    private val objectMapper: ObjectMapper
) : KeyDataLoader<URL, DeploymentSpec> {

    override suspend fun getByKey(key: URL, ctx: MyGraphQLContext): DeploymentSpec {
        return booberWebClient.get<JsonNode>("token", key.toString())
            .map { DeploymentSpec(objectMapper.writeValueAsString(it)) }
            .toMono()
            .blockNonNullAndHandleError()
    }
}
