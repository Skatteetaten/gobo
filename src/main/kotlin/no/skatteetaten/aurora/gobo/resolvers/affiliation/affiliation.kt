package no.skatteetaten.aurora.gobo.resolvers.affiliation

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor

data class Affiliation(val name: String)

data class AffiliationEdge(private val node: Affiliation) : DefaultEdge<Affiliation>(node, Cursor(node.name))

data class AffiliationsConnection(override val edges: List<AffiliationEdge>, override val pageInfo: PageInfo?) :
    Connection<AffiliationEdge>()
