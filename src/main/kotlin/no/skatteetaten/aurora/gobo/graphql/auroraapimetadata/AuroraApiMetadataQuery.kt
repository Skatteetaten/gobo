package no.skatteetaten.aurora.gobo.graphql.auroraapimetadata

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.AuroraApiMetadataService
import org.springframework.stereotype.Component

@Component
class AuroraApiMetadataQuery(
    private val service: AuroraApiMetadataService
) : Query {

    suspend fun auroraApiMetadata(dfe: DataFetchingEnvironment): AuroraApiMetadata {
        val clientConfig = service.getClientConfig()
        return AuroraApiMetadata(clientConfig)
    }
}
