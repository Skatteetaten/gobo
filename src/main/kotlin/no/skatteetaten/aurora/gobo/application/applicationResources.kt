package no.skatteetaten.aurora.gobo.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.hateoas.ResourceSupport

@JsonIgnoreProperties(ignoreUnknown = true)
data class StatusResource(val code: String, val comment: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionResource(val deployTag: String, val auroraVersion: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitInfoResource(val commitId: String?, val commitTime: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ImageDetailsResource(val imageBuildTime: String?, val dockerImageReference: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationInstanceDetailsResource(
    val buildTime: String?,
    val gitInfo: GitInfoResource,
    val imageDetails: ImageDetailsResource
) : ResourceSupport()

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationInstanceResource(
    val affiliation: String,
    val environment: String,
    val namespace: String,
    val status: StatusResource,
    val version: VersionResource
) : ResourceSupport()

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationResource(
    val name: String,
    val tags: List<String>,
    val applicationInstances: List<ApplicationInstanceResource>
)
