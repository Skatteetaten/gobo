package no.skatteetaten.aurora.gobo.resolvers.auroraapimetadata

data class AuroraApiMetadata(
    val clientConfig: ClientConfig
)

data class ClientConfig(
    val gitUrlPattern: String,
    val openshiftCluster: String,
    val openshiftUrl: String,
    val apiVersion: Int
)
