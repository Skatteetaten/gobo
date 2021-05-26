package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.phil.PhilService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class DeploymentEnvironmentMutation(
    @Qualifier("philServiceReactive") private val philService: PhilService,
) : Mutation {

    suspend fun deployEnvironment(input: DeploymentEnvironmentInput, dfe: DataFetchingEnvironment): DeploymentEnvironmentResponse {
        val philDeploymentResult = philService.deployEnvironment(input.environment, dfe.token())
        return DeploymentEnvironmentResponse(philDeploymentResult.success)
    }
}

data class DeploymentEnvironmentInput(val environment: String)

data class DeploymentEnvironmentResponse(val success: Boolean)
