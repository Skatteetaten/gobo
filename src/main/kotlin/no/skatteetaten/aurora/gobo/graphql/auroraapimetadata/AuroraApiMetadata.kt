package no.skatteetaten.aurora.gobo.graphql.auroraapimetadata

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.load
import no.skatteetaten.aurora.gobo.integration.boober.ConfigNames

data class AuroraApiMetadata(
    val clientConfig: ClientConfig
) {
    suspend fun configNames(dfe: DataFetchingEnvironment): List<String> {
        return dfe.load<AuroraApiMetadata, ConfigNames>(this).names
    }
}

data class ClientConfig(
    val gitUrlPattern: String,
    val openshiftCluster: String,
    val openshiftUrl: String,
    val apiVersion: Int
)
