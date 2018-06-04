package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Edge
import no.skatteetaten.aurora.gobo.resolvers.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationsConnection
import org.springframework.util.Base64Utils

data class Affiliation(val name: String, val applications: ApplicationsConnection)

data class AffiliationEdge(val node: Affiliation) : Edge {
    override fun cursor(): String? = Base64Utils.encodeToString(node.name.toByteArray())
}

data class AffiliationsConnection(val edges: List<AffiliationEdge>, val pageInfo: PageInfo?) : Connection {
    override fun totalCount() = edges.size
}