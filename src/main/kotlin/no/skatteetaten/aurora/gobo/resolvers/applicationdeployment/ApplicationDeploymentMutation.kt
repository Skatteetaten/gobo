package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.reactive.awaitFirst
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

@Component
class ApplicationDeploymentMutation(
    private val applicationUpgradeService: ApplicationUpgradeService,
    private val applicationDeploymentService: ApplicationDeploymentService
) : Mutation {

    fun redeployWithVersion(input: ApplicationDeploymentVersionInput, dfe: DataFetchingEnvironment): Boolean {
        applicationUpgradeService.upgrade(dfe.token(), input.applicationDeploymentId, input.version)
        return true
    }

    fun redeployWithCurrentVersion(input: ApplicationDeploymentIdInput, dfe: DataFetchingEnvironment): Boolean {
        applicationUpgradeService.deployCurrentVersion(dfe.token(), input.applicationDeploymentId)
        return true
    }

    fun refreshApplicationDeployment(input: RefreshByApplicationDeploymentIdInput, dfe: DataFetchingEnvironment) =
        applicationUpgradeService.refreshApplicationDeployment(dfe.token(), input.applicationDeploymentId)

    fun refreshApplicationDeployments(input: RefreshByAffiliationsInput, dfe: DataFetchingEnvironment) =
        applicationUpgradeService.refreshApplicationDeployments(dfe.token(), input.affiliations)

    fun deleteApplicationDeployment(input: DeleteApplicationDeploymentInput, dfe: DataFetchingEnvironment) =
        applicationDeploymentService.deleteApplicationDeployment(dfe.token(), input)
}

@Component
class ApplicationDeploymentQuery(
    private val applicationService: ApplicationService
) : Query {

    suspend fun applicationDeployment(id: String): ApplicationDeployment? =
        applicationService.getApplicationDeployment(id).awaitFirst().let { resource ->
            val imageRepo = resource.imageRepository()
            ApplicationDeployment.create(resource, imageRepo)
        }

    suspend fun applicationDeployments(applicationDeploymentRefs: List<ApplicationDeploymentRef>): List<ApplicationDeployment> {
        return applicationService.getApplicationDeployment(applicationDeploymentRefs).awaitFirst().map { resource ->
            val imageRepo = resource.imageRepository()
            ApplicationDeployment.create(resource, imageRepo)
        }
    }

    private fun ApplicationDeploymentResource.imageRepository() =
        this.dockerImageRepo.takeIf { it != null && !DockerRegistryUtil.isInternal(it) }
            ?.let { ImageRepository.fromRepoString(it) }

    suspend fun application(applicationDeployment: ApplicationDeployment): Application? {
        val application = applicationService.getApplication(applicationDeployment.applicationId).awaitFirst()
        return createApplicationEdge(application).node
    }
}
