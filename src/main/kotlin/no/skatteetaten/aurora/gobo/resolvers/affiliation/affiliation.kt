package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.resolvers.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationsConnection

data class Affiliation(val name: String, val applications: ApplicationsConnection)

data class AffiliationEdge(val cursor: String, val node: Affiliation)

data class AffiliationsConnection(val edges: List<AffiliationEdge>, val count: Int, val pageInfo: PageInfo?)