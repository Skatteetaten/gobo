package no.skatteetaten.aurora.gobo.resolvers.application

data class Status(val code: String, val comment: String?)

data class Version(val deployTag: String, val auroraVersion: String?)

data class Application(
    val affiliation: String,
    val environment: String,
    val name: String,
    val status: Status,
    val version: Version
)
