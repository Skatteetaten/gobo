package no.skatteetaten.aurora.gobo.integration.mokey

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.hateoas.ResourceSupport
import org.springframework.web.util.UriUtils
import java.nio.charset.Charset
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class StatusResource(val code: String, val comment: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionResource(val deployTag: String, val auroraVersion: String?, val releaseTo: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitInfoResource(val commitId: String?, val commitTime: Instant?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImageDetailsResource(
    val imageBuildTime: Instant?,
    val dockerImageReference: String?,
    val dockerImageTagReference: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
class PodResourceResource(
    val name: String,
    val phase: String,
    val startTime: Instant,
    val replicaName: String?,
    val latestReplicaName: Boolean,
    val managementResponses: ManagementResponsesResource?,
    val containers: List<ContainerResource>,
    val deployTag: String?,
    val latestDeployTag: Boolean

) : ResourceSupport()

@JsonIgnoreProperties(ignoreUnknown = true)
data class DeployDetailsResource(
    val targetReplicas: Int,
    val availableReplicas: Int,
    val deployment: String? = null,
    val phase: String? = null,
    val deployTag: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ContainerResource(
    val name: String,
    val state: String,
    val image: String,
    val restartCount: Int,
    val ready: Boolean = false
)

data class ManagementEndpointResponseResource(
    val hasResponse: Boolean,
    val textResponse: String? = null,
    val httpCode: Int? = null,
    val createdAt: Instant = Instant.now(),
    val url: String? = null,
    val error: ManagementEndpointErrorResource? = null
)

data class ManagementResponsesResource(
    val links: ManagementEndpointResponseResource,
    val health: ManagementEndpointResponseResource?,
    val info: ManagementEndpointResponseResource?,
    val env: ManagementEndpointResponseResource?
)

data class ManagementEndpointErrorResource(
    val code: String,
    val message: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationDeploymentDetailsResource(
    val buildTime: Instant?,
    val gitInfo: GitInfoResource?,
    val imageDetails: ImageDetailsResource?,
    val podResources: List<PodResourceResource>,
    val dependencies: Map<String, String> = emptyMap(),
    val applicationDeploymentCommand: ApplicationDeploymentCommandResource,
    val deployDetails: DeployDetailsResource?
) : ResourceSupport() {

    fun link(rel: String) = this.findLink(rel)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationDeploymentCommandResource(
    val overrideFiles: Map<String, String> = emptyMap(),
    val applicationDeploymentRef: ApplicationDeploymentRefResource,
    val auroraConfig: AuroraConfigRefResource
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationDeploymentRefResource(val environment: String, val application: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AuroraConfigRefResource(
    val name: String,
    val refName: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationDeploymentResource(
    val identifier: String,
    val name: String,
    val affiliation: String,
    val environment: String,
    val namespace: String,
    val status: StatusResource,
    val version: VersionResource,
    val dockerImageRepo: String?,
    val time: Instant
) : ResourceSupport() {

    private val APPLICATION_REL = "Application"

    val applicationId: String
        get() = findLink(APPLICATION_REL).idPart

    private val String.idPart: String get() = split("/").last()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationResource(
    val identifier: String,
    val name: String,
    val applicationDeployments: List<ApplicationDeploymentResource>
) : ResourceSupport()

data class RefreshParams(val applicationDeploymentId: String? = null, val affiliations: List<String>? = null)

private fun ResourceSupport.findLink(rel: String): String {
    return links.firstOrNull { it.rel == rel }?.href?.let {
        UriUtils.decode(it, Charset.defaultCharset())
    } ?: throw SourceSystemException("Link with rel $rel was not found")
}
