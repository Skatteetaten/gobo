package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import com.expediagroup.graphql.server.operations.Mutation
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.springframework.stereotype.Component

data class DeployResponse(val applicationDeploymentId: String)

@Component
class ApplicationDeploymentMutation(
    private val applicationUpgradeService: ApplicationUpgradeService,
    private val applicationDeploymentService: ApplicationDeploymentService
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

    suspend fun deleteApplicationDeployment(input: DeleteApplicationDeploymentInput, dfe: DataFetchingEnvironment): Boolean {
        applicationDeploymentService.deleteApplicationDeployment(dfe.token(), input)
        return true
    }
}
