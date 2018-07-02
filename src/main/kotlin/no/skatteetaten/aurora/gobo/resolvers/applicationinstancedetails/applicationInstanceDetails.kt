package no.skatteetaten.aurora.gobo.resolvers.applicationinstancedetails

import no.skatteetaten.aurora.gobo.application.ApplicationInstanceDetailsResource

data class GitInfo(
    val commitId: String?,
    val commitTime: String?
)

data class ImageDetails(
    val imageBuildTime: String?,
    val dockerImageReference: String?
)

data class PodResource(
    val name: String,
    val status: String,
    val restartCount: Int,
    val ready: Boolean,
    val startTime: String,
    val metricsUrl: String?,
    val splunkUrl: String?,
    val links: List<Link>
)

data class Link(val name: String, val url: String)

data class ApplicationInstanceDetails(
    val buildTime: String? = null,
    val imageDetails: ImageDetails,
    val gitInfo: GitInfo,
    val podResources: List<PodResource>
)

fun createApplicationInstanceDetails(resource: ApplicationInstanceDetailsResource): ApplicationInstanceDetails {
    val metricsUrl = resource.getLink("metrics")?.href
    val splunkUrl = resource.getLink("splunk")?.href
    val links = resource.links.filter { it.rel != "metrics" && it.rel != "splunk" }.map {
        Link(it.rel, it.href)
    }

    return ApplicationInstanceDetails(
        buildTime = resource.buildTime,
        imageDetails = resource.imageDetails.let {
            ImageDetails(it.imageBuildTime, it.dockerImageReference)
        },
        gitInfo = resource.gitInfo.let {
            GitInfo(it.commitId, it.commitTime)
        },
        podResources = resource.podResources.map {
            PodResource(
                it.name,
                it.status,
                it.restartCount,
                it.ready,
                it.startTime,
                metricsUrl,
                splunkUrl,
                links
            )
        }
    )
}