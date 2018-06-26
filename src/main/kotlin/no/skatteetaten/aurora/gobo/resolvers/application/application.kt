package no.skatteetaten.aurora.gobo.resolvers.application

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.application.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.ApplicationInstance
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.Status
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.Version

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

fun createApplicationEdge(resource: ApplicationResource): ApplicationEdge {
    val applicationInstances = resource.applicationInstances.map {
        ApplicationInstance(
            it.affiliation,
            it.environment,
            it.namespace,
            Status(it.status.code, it.status.comment),
            Version(
                it.version.deployTag,
                it.version.auroraVersion
            )
        )
    }

    return ApplicationEdge(
        Application(
            resource.name,
            resource.tags,
            applicationInstances
        )
    )
}