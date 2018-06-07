package no.skatteetaten.aurora.gobo.resolvers.affiliation

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationsConnection
import org.springframework.util.Base64Utils

data class Affiliation(val name: String, val applications: ApplicationsConnection)

data class AffiliationEdge(private val node: Affiliation) : DefaultEdge<Affiliation>(node, {
    Base64Utils.encodeToString(node.name.toByteArray())
})

data class AffiliationsConnection(override val edges: List<AffiliationEdge>, override val pageInfo: PageInfo?) :
    Connection<AffiliationEdge>()
