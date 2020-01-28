package no.skatteetaten.aurora.gobo.resolvers.auroraapimetadata

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.boober.AuroraApiMetadataService
import org.springframework.stereotype.Component

@Component
class AuroraApiMetadataQueryResolver(
    private val service: AuroraApiMetadataService
) : GraphQLQueryResolver {

    fun auroraApiMetadata(dfe: DataFetchingEnvironment): AuroraApiMetadata {
        val clientConfig = service.getClientConfig()
        return AuroraApiMetadata(clientConfig)
    }
}

@Component
class AuroraApiMetadataResolver(
    private val service: AuroraApiMetadataService
) : GraphQLResolver<AuroraApiMetadata> {

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
