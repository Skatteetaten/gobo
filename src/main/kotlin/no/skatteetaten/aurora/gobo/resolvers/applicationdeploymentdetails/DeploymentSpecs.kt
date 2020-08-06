package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import java.net.URL

class DeploymentSpecs(
    val deploymentSpecCurrent: URL?,
    val deploymentSpecDeployed: URL?
)

data class DeploymentSpec(val jsonRepresentation: String)

/*
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
*/
