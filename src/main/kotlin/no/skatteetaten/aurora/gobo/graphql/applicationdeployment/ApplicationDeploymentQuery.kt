package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import com.expediagroup.graphql.server.operations.Query
import no.skatteetaten.aurora.gobo.graphql.application.Application
import no.skatteetaten.aurora.gobo.graphql.application.DockerRegistryUtil
import no.skatteetaten.aurora.gobo.graphql.application.createApplicationEdge
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

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
        return applicationService.getApplicationDeployments(applicationDeploymentRefs).map { resource ->
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
