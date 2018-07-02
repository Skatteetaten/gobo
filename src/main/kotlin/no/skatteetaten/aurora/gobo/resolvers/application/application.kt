package no.skatteetaten.aurora.gobo.resolvers.application

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.application.ApplicationInstanceDetailsResource
import no.skatteetaten.aurora.gobo.application.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.ApplicationInstance
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.createApplicationInstances

data class Application(
    val name: String,
    val tags: List<String>,
    val applicationInstances: List<ApplicationInstance>
)

data class ApplicationEdge(private val node: Application) : DefaultEdge<Application>(
    node,
    Cursor(node.name)
)

data class ApplicationsConnection(override val edges: List<ApplicationEdge>, override val pageInfo: PageInfo?) :
    Connection<ApplicationEdge>()

fun createApplicationEdge(
    resource: ApplicationResource,
    details: List<ApplicationInstanceDetailsResource>
): ApplicationEdge {
    val applicationInstances = createApplicationInstances(resource, details)
    return ApplicationEdge(
        Application(
            resource.name,
            resource.tags,
            applicationInstances
        )
    )
}