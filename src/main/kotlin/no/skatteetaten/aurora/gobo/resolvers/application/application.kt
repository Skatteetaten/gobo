package no.skatteetaten.aurora.gobo.resolvers.application

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.application.ApplicationInstanceDetailsResource
import no.skatteetaten.aurora.gobo.application.ApplicationResource
import no.skatteetaten.aurora.gobo.application.ImageRepo
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor
import no.skatteetaten.aurora.gobo.resolvers.PagedEdges
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.ApplicationInstance
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.createApplicationInstances

data class Application(
    val name: String,
    val applicationInstances: List<ApplicationInstance>
)

data class ApplicationEdge(private val node: Application) : DefaultEdge<Application>(
    node,
    Cursor(node.name)
) {
    companion object {
        fun create(resource: ApplicationResource, applicationInstances: List<ApplicationInstance>) =
            ApplicationEdge(
                Application(
                    resource.name,
                    applicationInstances
                )
            )
    }
}

data class ApplicationsConnection(override val edges: List<ApplicationEdge>, override val pageInfo: PageInfo?) :
    Connection<ApplicationEdge>()

data class ImageTag(
    val imageRepo: ImageRepo,
    val name: String
) {
    val type: ImageTagType
        get() {
            return if (name.equals("latest")) ImageTagType.LATEST
            else if (name.endsWith("-SNAPSHOT")) ImageTagType.SNAPSHOT
            else if (name.matches(Regex("^\\d+$"))) ImageTagType.MAJOR
            else if (name.matches(Regex("^\\d+.\\d+$"))) ImageTagType.MINOR
            else if (name.matches(Regex("^\\d+.\\d+.\\d+$"))) ImageTagType.BUGFIX
            else ImageTagType.AURORA_VERSION
        }
}

data class ImageTagEdge(private val node: ImageTag) : DefaultEdge<ImageTag>(node, Cursor(node.name))

data class ImageTagsConnection(
    override val edges: List<ImageTagEdge>,
    override val pageInfo: PageInfo?,
    override val totalCount: Int = edges.size
) : Connection<ImageTagEdge>() {
    constructor(paged: PagedEdges<ImageTagEdge>) : this(paged.edges, paged.pageInfo, paged.totalCount)
}

enum class ImageTagType {
    LATEST,
    SNAPSHOT,
    MAJOR,
    MINOR,
    BUGFIX,
    AURORA_VERSION
}

fun createApplicationEdge(
    resource: ApplicationResource,
    details: List<ApplicationInstanceDetailsResource>
): ApplicationEdge {
    val applicationInstances = createApplicationInstances(resource, details)
    return ApplicationEdge.create(resource, applicationInstances)
}