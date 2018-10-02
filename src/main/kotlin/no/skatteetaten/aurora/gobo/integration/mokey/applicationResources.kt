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
data class ImageDetailsResource(val imageBuildTime: Instant?, val dockerImageReference: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PodResourceResource(
    val name: String,
    val status: String,
    val restartCount: Int,
    val ready: Boolean,
    val startTime: Instant,
    val managementResponses: ManagementResponsesResource?
) : ResourceSupport()

data class ManagementResponsesResource(
    val health: HttpResponseResource?,
    val info: HttpResponseResource?
)

data class HttpResponseResource(
    val textResponse: String,
    val createdAt: Instant
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationDeploymentDetailsResource(
    val buildTime: Instant?,
    val gitInfo: GitInfoResource?,
    val imageDetails: ImageDetailsResource?,
    val podResources: List<PodResourceResource>,
    val applicationDeploymentCommand: ApplicationDeploymentCommandResource
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

data class RefreshParams(val applicationDeploymentId: String)

private fun ResourceSupport.findLink(rel: String): String {
    return links.firstOrNull { it.rel == rel }?.href?.let {
        UriUtils.decode(it, Charset.defaultCharset())
    } ?: throw SourceSystemException("Link with rel $rel was not found")
}
