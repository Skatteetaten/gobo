package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.resolvers.GoboConnection
import no.skatteetaten.aurora.gobo.resolvers.GoboEdge
import no.skatteetaten.aurora.gobo.resolvers.GoboPageInfo

data class Affiliation(val name: String)

data class AffiliationEdge(val node: Affiliation) : GoboEdge(node.name)

data class AffiliationsConnection(override val edges: List<AffiliationEdge>, override val pageInfo: GoboPageInfo?) :
    GoboConnection<AffiliationEdge>()
