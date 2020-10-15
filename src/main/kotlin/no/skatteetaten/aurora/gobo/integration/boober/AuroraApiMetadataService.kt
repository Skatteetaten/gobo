package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.graphql.auroraapimetadata.ClientConfig
import org.springframework.stereotype.Service

@Service
class AuroraApiMetadataService(private val booberWebClient: BooberWebClient) {

    suspend fun getClientConfig() =
        booberWebClient.get<ClientConfig>("/v1/clientconfig").response()

    suspend fun getConfigNames() =
        booberWebClient.get<String>("/v1/auroraconfignames").responses()
}
