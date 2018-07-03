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
    val links: List<Link>
) {
    fun links(names: List<String>?): List<Link> {
        return if (names == null) {
            links
        } else {
            links.filter { names.contains(it.name) }
        }
    }
}

data class Link(val name: String, val url: String) {
    companion object {
        fun create(link: org.springframework.hateoas.Link) = Link(link.rel, link.href)
    }
}

data class ApplicationInstanceDetails(
    val buildTime: String? = null,
    val imageDetails: ImageDetails,
    val gitInfo: GitInfo,
    val podResources: List<PodResource>
) {
    companion object {
        fun create(resource: ApplicationInstanceDetailsResource): ApplicationInstanceDetails {
            return ApplicationInstanceDetails(
                buildTime = resource.buildTime,
                imageDetails = resource.imageDetails.let { ImageDetails(it.imageBuildTime, it.dockerImageReference) },
                gitInfo = resource.gitInfo.let { GitInfo(it.commitId, it.commitTime) },
                podResources = resource.podResources.map {
                    PodResource(
                        it.name,
                        it.status,
                        it.restartCount,
                        it.ready,
                        it.startTime,
                        resource.links.map { Link.create(it) }
                    )
                }
            )
        }
    }
}