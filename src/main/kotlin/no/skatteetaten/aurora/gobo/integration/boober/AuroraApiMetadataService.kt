package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.resolvers.auroraapimetadata.ClientConfig
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import org.springframework.stereotype.Service
import reactor.kotlin.core.publisher.toMono

@Service
class AuroraApiMetadataService(private val booberWebClient: BooberWebClient) {

    fun getClientConfig(): ClientConfig {
        return booberWebClient
            .anonymousGet<ClientConfig>("/v1/auroraconfignames")
            .toMono()
            .blockNonNullAndHandleError()
    }
}