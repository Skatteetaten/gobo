package no.skatteetaten.aurora.gobo.resolvers.application

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.application.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor

data class Status(val code: String, val comment: String?)

data class Version(val deployTag: String, val auroraVersion: String?)

data class Application(
    val affiliationId: String,
    val environment: String,
    val namespaceId: String,
    val name: String,
    val status: Status,
    val version: Version
)

data class ApplicationEdge(private val node: Application) : DefaultEdge<Application>(
    node,
    Cursor("${node.affiliationId}::${node.environment}::${node.name}")
)

data class ApplicationsConnection(override val edges: List<ApplicationEdge>, override val pageInfo: PageInfo?) :
    Connection<ApplicationEdge>()

fun createApplicationEdge(resource: ApplicationResource) =
    ApplicationEdge(
        Application(
            resource.affiliation,
            resource.environment,
            resource.namespace,
            resource.name,
            Status(resource.status.code, resource.status.comment),
            Version(resource.version.deployTag, resource.version.auroraVersion)
        )
    )