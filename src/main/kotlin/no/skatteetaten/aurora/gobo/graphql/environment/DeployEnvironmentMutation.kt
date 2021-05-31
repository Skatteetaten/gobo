package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import java.time.Instant
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.phil.DeploymentResource
import no.skatteetaten.aurora.gobo.integration.phil.PhilService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component

@Component
class DeploymentEnvironmentMutation(
    private val philService: PhilService,
) : Mutation {

    suspend fun deployEnvironment(
        input: DeploymentEnvironmentInput,
        dfe: DataFetchingEnvironment
    ): List<Deployment>? {
        dfe.checkValidUserToken()
        return philService.deployEnvironment(input.environment, dfe.token())
            .let { it.toDeploymentEnvironmentResponse() }
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
}

data class DeploymentEnvironmentInput(val environment: String)

data class DeploymentRef(
    val cluster: String,
    val affiliation: String,
    val environment: String,
    val application: String
)

data class Deployment(
    val deploymentRef: DeploymentRef,
    val deployId: String,
    val timestamp: Instant,
    val message: String,
)
