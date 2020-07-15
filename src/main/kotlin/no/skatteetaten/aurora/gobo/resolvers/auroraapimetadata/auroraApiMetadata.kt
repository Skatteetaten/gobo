package no.skatteetaten.aurora.gobo.resolvers.auroraapimetadata

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.AuroraApiMetadataService
import org.springframework.stereotype.Component

@Component
class AuroraApiMetadataQueryResolver(
    private val service: AuroraApiMetadataService
) : Query {

    fun auroraApiMetadata(dfe: DataFetchingEnvironment): AuroraApiMetadata {
        val clientConfig = service.getClientConfig()
        return AuroraApiMetadata(clientConfig)
    }
}

@Component
class AuroraApiMetadataResolver(
    private val service: AuroraApiMetadataService
) : Query {

    fun configNames(auroraApiMetadata: AuroraApiMetadata): List<String> {
        return service.getConfigNames()
    }
}

data class AuroraApiMetadata(
    val clientConfig: ClientConfig
)

data class ClientConfig(
    val gitUrlPattern: String,
    val openshiftCluster: String,
    val openshiftUrl: String,
    val apiVersion: Int
)
