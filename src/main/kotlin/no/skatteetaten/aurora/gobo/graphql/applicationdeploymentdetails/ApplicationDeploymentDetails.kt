package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadOrThrow
import java.net.URL
import java.time.Instant
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ManagementEndpointResponseResource
import no.skatteetaten.aurora.gobo.integration.mokey.PodResourceResource
import no.skatteetaten.aurora.gobo.integration.mokey.optionalLink
import no.skatteetaten.aurora.gobo.integration.mokey.toGoboLinks

data class GitInfo(
    val commitId: String?,
    val commitTime: Instant?
)

data class Container(
    val name: String,
    val state: String,
    val image: String,
    val restartCount: Int = 0,
    val ready: Boolean = false
)

data class PodResource(
    val name: String,
    val phase: String,
    val startTime: Instant? = null,
    val replicaName: String?,
    val latestReplicaName: Boolean,
    val deployTag: String?,
    val latestDeployTag: Boolean,
    val links: List<Link>,
    val managementResponses: ManagementResponses?,
    val containers: List<Container>,
    val ready: Boolean = containers.all { it.ready },
    val restartCount: Int = containers.sumBy { it.restartCount }
) {
    companion object {
        fun create(resource: PodResourceResource) =
            PodResource(
                name = resource.name,
                phase = resource.phase,
                startTime = resource.startTime,
                links = resource._links.toGoboLinks(),
                managementResponses = resource.managementResponses?.let { managementResponses ->
                    val links = ManagementEndpointResponse.create(managementResponses.links)
                    val health = managementResponses.health?.let { ManagementEndpointResponse.create(it) }
                    val info = managementResponses.info?.let { ManagementEndpointResponse.create(it) }
                    val env = managementResponses.env?.let { ManagementEndpointResponse.create(it) }
                    ManagementResponses(links, health, info, env)
                },
                replicaName = resource.replicaName,
                latestReplicaName = resource.latestReplicaName,
                deployTag = resource.deployTag,
                latestDeployTag = resource.latestDeployTag,
                containers = resource.containers.map {
                    Container(it.name, it.state, it.image, it.restartCount, it.ready)
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
                hasResponse = resource.hasResponse,
                textResponse = resource.textResponse,
                createdAt = resource.createdAt,
                httpCode = resource.httpCode,
                url = resource.url,
                error = resource.error?.let { ManagementEndpointError(it.code, it.message) }
            )
    }
}

data class ManagementEndpointError(
    val code: String,
    val message: String? = null
)

class Link(val name: String, val url: URL) {
    companion object {
        fun create(name: String, url: String): Link {
            val href = if (url.matches("https?://.*".toRegex())) {
                url
            } else {
                "http://$url"
            }
            return Link(name, URL(href))
        }
    }
}

data class ApplicationDeploymentDetails(
    val updatedBy: String?,
    val buildTime: Instant?,
    val imageDetails: ImageDetails?,
    val gitInfo: GitInfo?,
    val databases: List<String>,
    val podResources: List<PodResource>,
    val deploymentSpecs: DeploymentSpecs,
    val deployDetails: DeployDetails?,
    val serviceLinks: List<Link>
) {
    companion object {
        fun create(resource: ApplicationDeploymentDetailsResource): ApplicationDeploymentDetails {
            return ApplicationDeploymentDetails(
                updatedBy = resource.updatedBy,
                buildTime = resource.buildTime,
                imageDetails = resource.imageDetails?.let {
                    ImageDetails(
                        imageBuildTime = it.imageBuildTime,
                        digest = it.dockerImageReference?.substringAfterLast("@"),
                        dockerImageTagReference = it.dockerImageTagReference
                    )
                },
                databases = resource.databases ?: listOf(),
                gitInfo = resource.gitInfo?.let { GitInfo(it.commitId, it.commitTime) },
                podResources = resource.podResources.map { PodResource.create(it) },
                deploymentSpecs = DeploymentSpecs(
                    deploymentSpecCurrent = resource.optionalLink("DeploymentSpecCurrent")?.let { URL(it.href) },
                    deploymentSpecDeployed = resource.optionalLink("DeploymentSpecDeployed")?.let { URL(it.href) }
                ),
                deployDetails = resource.deployDetails?.let {
                    DeployDetails(
                        targetReplicas = it.targetReplicas,
                        availableReplicas = it.availableReplicas,
                        deployment = it.deployment,
                        phase = it.phase,
                        deployTag = it.deployTag,
                        paused = it.paused
                    )
                },
                serviceLinks = resource.serviceLinks.toGoboLinks()
            )
        }
    }
}

data class DeploymentSpec(val jsonRepresentation: String)

class DeploymentSpecs(
    val deploymentSpecCurrent: URL?,
    val deploymentSpecDeployed: URL?
) {
    suspend fun current(dfe: DataFetchingEnvironment): DeploymentSpec? =
        deploymentSpecCurrent?.let { dfe.loadOrThrow(it) }

    suspend fun deployed(dfe: DataFetchingEnvironment): DeploymentSpec? =
        deploymentSpecDeployed?.let { dfe.loadOrThrow(it) }
}

data class DeployDetails(
    val targetReplicas: Int,
    val availableReplicas: Int,
    val deployment: String? = null,
    val phase: String? = null,
    val deployTag: String? = null,
    val paused: Boolean = false
)
