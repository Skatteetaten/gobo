package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.resolvers.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation

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

data class ApplicationEdge(val cursor: String, val node: Application)

data class ApplicationsConnection(val edges: List<ApplicationEdge>, val count: Int, val pageInfo: PageInfo?)
