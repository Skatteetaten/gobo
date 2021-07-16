package no.skatteetaten.aurora.gobo.graphql.auroraapimetadata

import com.expediagroup.graphql.server.operations.Query
import no.skatteetaten.aurora.gobo.integration.boober.AuroraApiMetadataService
import org.springframework.stereotype.Component

@Component
class AuroraApiMetadataQuery(
    private val service: AuroraApiMetadataService
) : Query {

    suspend fun auroraApiMetadata(): AuroraApiMetadata {
        val clientConfig = service.getClientConfig()
        return AuroraApiMetadata(clientConfig)
    }
}
