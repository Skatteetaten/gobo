package no.skatteetaten.aurora.gobo.graphql.environment

import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import java.time.Instant
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.phil.DeletionResource
import no.skatteetaten.aurora.gobo.integration.phil.PhilService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component

@Component
class DeleteEnvironmentMutation(
    private val philService: PhilService
) : Mutation {
    suspend fun deleteEnvironment(
        input: DeleteEnvironmentInput,
        dfe: DataFetchingEnvironment
    ): List<DeleteEnvironmentResult>? {
        dfe.checkValidUserToken()
        return philService.deleteEnvironment(input.environment, dfe.token())
            .let { it.toDeleteEnvironmentResult() }
    }

    private fun List<DeletionResource>?.toDeleteEnvironmentResult() =
        this?.map {
            DeleteEnvironmentResult(
                deploymentRef = DeploymentRef(
                    it.deploymentRef.cluster,
                    it.deploymentRef.affiliation,
                    it.deploymentRef.environment,
                    it.deploymentRef.application
                ),
                timestamp = it.timestamp.toInstant(),
                message = it.message,
                deleted = it.deleted
            )
        }
}

data class DeleteEnvironmentInput(val environment: String)

data class DeleteEnvironmentResult(
    val deploymentRef: DeploymentRef,
    val timestamp: Instant,
    val message: String?,
    val deleted: Boolean
)
