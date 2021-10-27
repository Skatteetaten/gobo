package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import com.expediagroup.graphql.server.operations.Mutation
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.boober.BooberDeleteResponse
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.springframework.stereotype.Component

data class DeployResponse(val applicationDeploymentId: String)

@Component
class ApplicationDeploymentMutation(
    private val applicationUpgradeService: ApplicationUpgradeService,
    private val applicationDeploymentService: ApplicationDeploymentService,
    private val applicationService: ApplicationService
) : Mutation {

    suspend fun redeployWithVersion(input: ApplicationDeploymentVersionInput, dfe: DataFetchingEnvironment): DeployResponse {
        val id = applicationUpgradeService.upgrade(dfe.token(), input.applicationDeploymentId, input.version)
        return DeployResponse(id)
    }

    suspend fun redeployWithCurrentVersion(input: ApplicationDeploymentIdInput, dfe: DataFetchingEnvironment): DeployResponse {
        val id = applicationUpgradeService.deployCurrentVersion(dfe.token(), input.applicationDeploymentId)
        return DeployResponse(id)
    }

    suspend fun refreshApplicationDeployment(input: RefreshByApplicationDeploymentIdInput, dfe: DataFetchingEnvironment): Boolean {
        applicationUpgradeService.refreshApplicationDeployment(dfe.token(), input.applicationDeploymentId)
        return true
    }

    suspend fun refreshApplicationDeployments(input: RefreshByAffiliationsInput, dfe: DataFetchingEnvironment): Boolean {
        applicationUpgradeService.refreshApplicationDeployments(dfe.token(), input.affiliations)
        return true
    }

    @Deprecated(message = "use deleteApplicationDeployments instead")
    suspend fun deleteApplicationDeployment(input: DeleteApplicationDeploymentInput, dfe: DataFetchingEnvironment): Boolean {
        applicationDeploymentService.deleteApplicationDeployment(dfe.token(), input)
        return true
    }

    suspend fun deleteApplicationDeployments(
        input: DeleteApplicationDeploymentsInput,
        dfe: DataFetchingEnvironment
    ): DataFetcherResult<List<DeleteApplicationDeploymentsResult>> =
        applicationDeploymentService.deleteApplicationDeployments(
            dfe.token(),
            input.toDeleteApplicationDeploymentInputList()
        ).toDataFetcherResult()

    private suspend fun DeleteApplicationDeploymentsInput.toDeleteApplicationDeploymentInputList() =
        applicationService.getApplicationDeployments(this.applicationDeployments)
            .map { DeleteApplicationDeploymentInput(it.namespace, it.name) }

    private fun List<BooberDeleteResponse>.toDataFetcherResult() =
        DataFetcherResult.newResult<List<DeleteApplicationDeploymentsResult>>()
            .data(
                this.filter { it.success }
                    .map {
                        DeleteApplicationDeploymentsResult(
                            it.applicationRef.namespace,
                            it.applicationRef.name,
                            it.success
                        )
                    }
            )
            .errors(
                this.filter { !it.success }
                    .map {
                        GraphqlErrorBuilder.newError()
                            .message(it.reason)
                            .extensions(mapOf("resultObject" to it))
                            .build()
                    }
            )
            .build()
}
