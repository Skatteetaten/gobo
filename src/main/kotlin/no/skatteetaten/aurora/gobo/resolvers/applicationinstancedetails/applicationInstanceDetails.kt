package no.skatteetaten.aurora.gobo.resolvers.applicationinstancedetails

import no.skatteetaten.aurora.gobo.application.ApplicationInstanceDetailsResource
import no.skatteetaten.aurora.gobo.application.PodResourceResource
import java.net.URL
import java.time.Instant

data class GitInfo(
    val commitId: String?,
    val commitTime: Instant?
)

data class ImageDetails(
    val imageBuildTime: Instant?,
    val dockerImageReference: String?
) {
    val dockerImageRepo: String?
        get() = dockerImageReference?.replace(Regex("@.*$"), "")
}

data class PodResource(
    val name: String,
    val status: String,
    val restartCount: Int,
    val ready: Boolean,
    val startTime: Instant,
    val links: List<Link>
) {
    companion object {
        fun create(resource: PodResourceResource) =
            PodResource(
                resource.name,
                resource.status,
                resource.restartCount,
                resource.ready,
                resource.startTime,
                resource.links.map { Link.create(it) }
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

data class Link(val name: String, val url: URL) {
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

data class ApplicationInstanceDetails(
    val buildTime: Instant?,
    val imageDetails: ImageDetails?,
    val gitInfo: GitInfo?,
    val podResources: List<PodResource>
) {
    companion object {
        fun create(resource: ApplicationInstanceDetailsResource): ApplicationInstanceDetails {
            return ApplicationInstanceDetails(
                buildTime = resource.buildTime,
                imageDetails = resource.imageDetails?.let { ImageDetails(it.imageBuildTime, it.dockerImageReference) },
                gitInfo = resource.gitInfo?.let { GitInfo(it.commitId, it.commitTime) },
                podResources = resource.podResources.map { PodResource.create(it) }
            )
        }
    }
}