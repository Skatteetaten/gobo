package no.skatteetaten.aurora.gobo.resolvers.applicationinstance

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor

data class Status(val code: String, val comment: String?)

data class Version(val deployTag: String, val auroraVersion: String?)

data class ApplicationInstance(
    val affiliationId: String,
    val environment: String,
    val namespaceId: String,
    val status: Status,
    val version: Version
)

data class ApplicationInstanceEdge(private val node: ApplicationInstance) :
    DefaultEdge<ApplicationInstance>(node, Cursor("${node.affiliationId}::${node.environment}::${node.namespaceId}"))

data class ApplicationInstancesConnection(
    override val edges: List<ApplicationInstanceEdge>,
    override val pageInfo: PageInfo?
) :
    Connection<ApplicationInstanceEdge>()