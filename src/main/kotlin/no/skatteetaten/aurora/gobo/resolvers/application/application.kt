package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Edge
import no.skatteetaten.aurora.gobo.resolvers.PageInfo
import org.springframework.util.Base64Utils

data class Status(val code: String, val comment: String?)

data class Version(val deployTag: String, val auroraVersion: String?)

data class Application(
    val affiliation: String,
    val environment: String,
    val namespace: String,
    val name: String,
    val status: Status,
    val version: Version
)

data class ApplicationEdge(override val node: Application) : Edge<Application>() {
    override fun cursor(): String? =
        Base64Utils.encodeToString("${node.affiliation}::${node.environment}::${node.name}".toByteArray())
}

data class ApplicationsConnection(override val edges: List<ApplicationEdge>, override val pageInfo: PageInfo?) :
    Connection<ApplicationEdge>()
