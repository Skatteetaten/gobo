package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ManagementEndpointResponseResource
import no.skatteetaten.aurora.gobo.integration.mokey.PodResourceResource
import java.net.URL
import java.time.Instant

data class GitInfo(
    val commitId: String?,
    val commitTime: Instant?
)

data class ImageDetails(
    val imageBuildTime: Instant?,
    val dockerImageReference: String?
)

data class PodResource(
    val name: String,
    val status: String,
    val restartCount: Int,
    val ready: Boolean,
    val startTime: Instant,
    val links: List<Link>,
    val managementResponses: ManagementResponses?
) {
    companion object {
        fun create(resource: PodResourceResource) =
            PodResource(
                resource.name,
                resource.status,
                resource.restartCount,
                resource.ready,
                resource.startTime,
                resource.links.map { Link.create(it) },
                resource.managementResponses?.let { managementResponses ->
                    val links = ManagementEndpointResponse.create(managementResponses.links)
                    val health = managementResponses.health?.let { ManagementEndpointResponse.create(it) }
                    val info = managementResponses.info?.let { ManagementEndpointResponse.create(it) }
                    val env = managementResponses.env?.let { ManagementEndpointResponse.create(it) }
                    ManagementResponses(links, health, info, env)
                }
            )
    }

    fun links(names: List<String>?): List<Link> {
        return if (names == null) {
            links
        } else {
            links.filter { names.contains(it.name) }
        }
    }
}

data class ManagementResponses(
    val links: ManagementEndpointResponse,
    val health: ManagementEndpointResponse?,
    val info: ManagementEndpointResponse?,
    val env: ManagementEndpointResponse?
)

data class ManagementEndpointResponse(
    val hasResponse: Boolean,
    val textResponse: String?,
    val createdAt: Instant,
    val httpCode: Int?,
    val url: String?,
    val error: ManagementEndpointError?
) {
    companion object {
        fun create(resource: ManagementEndpointResponseResource) =
                ManagementEndpointResponse(
                        resource.hasResponse,
                        resource.textResponse,
                        resource.createdAt,
                        resource.httpCode,
                        resource.url,
                        resource.error?.let { ManagementEndpointError(it.code, it.message) }
                )
    }
}

data class ManagementEndpointError(
    val code: String,
    val message: String? = null
)

class Link private constructor(val name: String, val url: URL) {
    companion object {
        fun create(link: org.springframework.hateoas.Link): Link {
            val href = if (link.href.matches("https?://.*".toRegex())) {
                link.href
            } else {
                "http://${link.href}"
            }
            return Link(link.rel, URL(href))
        }
    }
}

data class DeploymentSpec(val jsonRepresentation: String)

class DeploymentSpecs(
    val deploymentSpecCurrent: URL?,
    val deploymentSpecDeployed: URL?
)

data class ApplicationDeploymentDetails(
    val buildTime: Instant?,
    val imageDetails: ImageDetails?,
    val gitInfo: GitInfo?,
    val podResources: List<PodResource>,
    val deploymentSpecs: DeploymentSpecs
) {
    companion object {
        fun create(resource: ApplicationDeploymentDetailsResource): ApplicationDeploymentDetails {
            return ApplicationDeploymentDetails(
                buildTime = resource.buildTime,
                imageDetails = resource.imageDetails?.let { ImageDetails(it.imageBuildTime, it.dockerImageReference) },
                gitInfo = resource.gitInfo?.let { GitInfo(it.commitId, it.commitTime) },
                podResources = resource.podResources.map { PodResource.create(it) },
                deploymentSpecs = DeploymentSpecs(
                    deploymentSpecCurrent = resource.getLink("DeploymentSpecCurrent")?.let { URL(it.href) },
                    deploymentSpecDeployed = resource.getLink("DeploymentSpecDeployed")?.let { URL(it.href) }
                )
            )
        }
    }
}