package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Edge
import no.skatteetaten.aurora.gobo.resolvers.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import org.springframework.util.Base64Utils

data class Status(val code: String, val comment: String?)

data class Version(val deployTag: String, val auroraVersion: String?)

data class Application(
    val affiliation: Affiliation,
    val environment: String,
    val namespace: Namespace,
    val name: String,
    val status: Status,
    val version: Version
)

data class ApplicationEdge(val node: Application) : Edge {
    override fun cursor(): String? =
        Base64Utils.encodeToString("${node.affiliation.name}::${node.environment}::${node.name}".toByteArray())
}

data class ApplicationsConnection(val edges: List<ApplicationEdge>, val pageInfo: PageInfo?) : Connection {
    override fun totalCount() = edges.size
}
