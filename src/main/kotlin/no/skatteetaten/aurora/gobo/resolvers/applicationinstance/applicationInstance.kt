package no.skatteetaten.aurora.gobo.resolvers.applicationinstance

data class Status(val code: String, val comment: String?)

data class Version(val deployTag: String, val auroraVersion: String?)

data class ApplicationInstance(
    val affiliationId: String,
    val environment: String,
    val namespaceId: String,
    val status: Status,
    val version: Version
)