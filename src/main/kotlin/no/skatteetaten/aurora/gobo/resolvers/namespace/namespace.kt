package no.skatteetaten.aurora.gobo.resolvers.namespace

import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.application.ApplicationsConnection

data class Namespace(val name: String, val affiliationId: String, val applications: ApplicationsConnection?)

data class NamespaceEdge(val cursor: String, val node: Namespace)

data class NamespaceConnection(val edges: List<NamespaceEdge>, val count: Int, val pageInfo: PageInfo?)
