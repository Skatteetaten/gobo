package no.skatteetaten.aurora.gobo.resolvers.applicationinstancedetails

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
    val metricsUrl: String?)

data class ApplicationInstanceDetails(
    val buildTime: String? = null,
    val imageDetails: ImageDetails,
    val gitInfo: GitInfo,
    val podResources: List<PodResource>
)