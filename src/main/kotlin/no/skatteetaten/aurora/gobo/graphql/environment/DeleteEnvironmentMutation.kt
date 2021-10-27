package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.phil.DeletionResource
import no.skatteetaten.aurora.gobo.integration.phil.EnvironmentService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DeleteEnvironmentMutation(
    private val environmentService: EnvironmentService
) : Mutation {
    suspend fun deleteEnvironment(
        input: DeleteEnvironmentInput,
        dfe: DataFetchingEnvironment
    ): List<DeleteEnvironmentResponse> {
        dfe.checkValidUserToken()

        return environmentService.deleteEnvironment(
            input.environment,
            dfe.token()
        ).let {
            it.toDeleteEnvironmentResult()
        } ?: emptyList()
    }

    private fun List<DeletionResource>?.toDeleteEnvironmentResult() = this?.map {
        DeleteEnvironmentResponse(
            deploymentRef = DeploymentRef(
                it.deploymentRef.cluster,
                it.deploymentRef.affiliation,
                it.deploymentRef.environment,
                it.deploymentRef.application
            ),
            timestamp = it.timestamp.toInstant(),
            message = it.message,
            deleted = it.deleted,
        )
    }
}

data class DeleteEnvironmentInput(val environment: String)

data class DeleteEnvironmentResponse(
    val deploymentRef: DeploymentRef,
    val timestamp: Instant,
    val message: String?,
    val deleted: Boolean
)
