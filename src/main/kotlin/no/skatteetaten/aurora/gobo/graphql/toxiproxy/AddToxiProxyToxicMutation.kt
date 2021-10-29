package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import com.expediagroup.graphql.server.operations.Mutation
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import org.springframework.stereotype.Component

@Component
class AddToxiProxyToxicMutation(
    private val toxiProxyToxicService: ToxiProxyToxicService
) : Mutation {

/*
    suspend fun addToxiProxyToxic(
        input: ToxiProxyToxicInput,
        dfe: DataFetchingEnvironment
    ): List<Deployment>? {
        dfe.checkValidUserToken()
        return null
//        environmentService.deployEnvironment(input.environment, dfe.token())
//            .let { it.toDeploymentEnvironmentResponse() }
    }

    private fun List<DeploymentResource>?.toDeploymentEnvironmentResponse() =
        this?.map {
            Deployment(
                deploymentRef = DeploymentRef(
                    it.deploymentRef.cluster,
                    it.deploymentRef.affiliation,
                    it.deploymentRef.environment,
                    it.deploymentRef.application
                ),
                deployId = it.deployId,
                timestamp = it.timestamp.toInstant(),
                message = it.message
            )
        }
*/
}

data class ToxiProxyToxicInput(val environment: String)

data class ToxiProxyToxicRef(
    val affiliation: String,
    val environment: String,
    val application: String
)
data class AddToxiProxyToxicsInput(
    val podName: String,
    val toxic: ToxicProxy
)
