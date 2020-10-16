package no.skatteetaten.aurora.gobo.integration.boober

import org.springframework.stereotype.Service

data class ClientConfig(
    val gitUrlPattern: String,
    val openshiftCluster: String,
    val openshiftUrl: String,
    val apiVersion: Int
)

@Service
class AuroraApiMetadataService(private val booberWebClient: BooberWebClient) {

    suspend fun getClientConfig() =
        booberWebClient.get<ClientConfig>("/v1/clientconfig").response()

    suspend fun getConfigNames() =
        booberWebClient.get<String>("/v1/auroraconfignames").responses()
}
