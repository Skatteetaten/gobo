package no.skatteetaten.aurora.gobo.service.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

data class Link(val rel: String, val href: String)

abstract class Resource(open val links: List<Link>) {
    fun getLink(rel: String): Link? = links.find { it.rel == rel }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class StatusResource(val code: String, val comment: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionResource(val deployTag: String, val auroraVersion: String?)

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
    override val links: List<Link>
) : Resource(links)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationInstanceDetailsResource(
    val buildTime: Instant?,
    val gitInfo: GitInfoResource?,
    val imageDetails: ImageDetailsResource?,
    val podResources: List<PodResourceResource>,
    override val links: List<Link>
) : Resource(links)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationInstanceResource(
    val affiliation: String,
    val environment: String,
    val namespace: String,
    val status: StatusResource,
    val version: VersionResource,
    override val links: List<Link>
) : Resource(links)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationResource(
    val name: String,
    val applicationInstances: List<ApplicationInstanceResource>
)
