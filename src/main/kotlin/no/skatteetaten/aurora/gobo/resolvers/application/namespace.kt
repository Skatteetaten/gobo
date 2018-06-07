package no.skatteetaten.aurora.gobo.resolvers.application

import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation

data class Namespace(val name: String, val affiliation: Affiliation, val applications: ApplicationsConnection?)

data class NamespaceEdge(val cursor: String, val node: Namespace)

data class NamespaceConnection(val edges: List<NamespaceEdge>, val count: Int, val pageInfo: PageInfo?)
