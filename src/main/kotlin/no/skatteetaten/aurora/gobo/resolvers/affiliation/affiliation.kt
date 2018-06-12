package no.skatteetaten.aurora.gobo.resolvers.affiliation

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor
import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationsConnection

data class Affiliation(val name: String, val applications: ApplicationsConnection)

data class AffiliationEdge(private val node: Affiliation) : DefaultEdge<Affiliation>(node, Cursor(node.name))

data class AffiliationsConnection(override val edges: List<AffiliationEdge>, override val pageInfo: PageInfo?) :
    Connection<AffiliationEdge>()
