package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import java.util.Date
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentResource
import no.skatteetaten.aurora.gobo.integration.phil.PhilService
import org.springframework.stereotype.Component

@Component
class DeploymentEnvironmentMutation(
    private val philService: PhilService,
) : Mutation {
    suspend fun deployEnvironment(
        input: DeploymentEnvironmentInput,
        dfe: DataFetchingEnvironment
    ): DeploymentEnvironmentResponse {
        val deploymentResourceList = philService.deployEnvironment(input.environment, dfe.token())
        return deploymentResourceList.toDeploymentEnvironmentResponse()
    }

    private fun List<DeploymentResource>?.toDeploymentEnvironmentResponse(): DeploymentEnvironmentResponse {
        val resultDeployments = if (this != null) {
            this.map {
                Deployment(
                    deploymentRef = DeploymentRef(
                        it.deploymentRef.cluster,
                        it.deploymentRef.affiliation,
                        it.deploymentRef.environment,
                        it.deploymentRef.application
                    ),
                    deployId = it.deployId,
                    timestamp = it.timestamp,
                    message = it.message
                )
            }
        } else {
            null
        }
        return DeploymentEnvironmentResponse(success = true, deployments = resultDeployments)
    }
}

data class DeploymentEnvironmentInput(val environment: String)

data class DeploymentEnvironmentResponse(val success: Boolean, val deployments: List<Deployment>?)

data class DeploymentRef(
    val cluster: String,
    val affiliation: String,
    val environment: String,
    val application: String
)

data class Deployment(
    val deploymentRef: DeploymentRef,
    val deployId: String = "",
    val timestamp: Date,
    val message: String,
)
