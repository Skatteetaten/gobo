package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.application.Application
import no.skatteetaten.aurora.gobo.resolvers.application.DockerRegistryUtil
import no.skatteetaten.aurora.gobo.resolvers.application.createApplicationEdge
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.resolvers.token
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

@Component
class ApplicationDeploymentQuery(
    private val applicationService: ApplicationService
) : Query {

    suspend fun applicationDeployment(id: String): ApplicationDeployment? =
        applicationService.getApplicationDeployment(id).let { resource ->
            val imageRepo = resource.imageRepository()
            ApplicationDeployment.create(resource, imageRepo)
        }

    suspend fun applicationDeployments(applicationDeploymentRefs: List<ApplicationDeploymentRef>): List<ApplicationDeployment> {
        return applicationService.getApplicationDeployment(applicationDeploymentRefs).map { resource ->
            val imageRepo = resource.imageRepository()
            ApplicationDeployment.create(resource, imageRepo)
        }
    }

    private fun ApplicationDeploymentResource.imageRepository() =
        this.dockerImageRepo.takeIf { it != null && !DockerRegistryUtil.isInternal(it) }
            ?.let { ImageRepository.fromRepoString(it) }

    suspend fun application(applicationDeployment: ApplicationDeployment): Application? {
        val application = applicationService.getApplication(applicationDeployment.applicationId)
        return createApplicationEdge(application).node
    }
}
