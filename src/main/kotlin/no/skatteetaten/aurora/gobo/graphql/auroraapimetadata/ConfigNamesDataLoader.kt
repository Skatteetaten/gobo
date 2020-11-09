package no.skatteetaten.aurora.gobo.graphql.auroraapimetadata

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.AuroraApiMetadataService
import no.skatteetaten.aurora.gobo.integration.boober.ConfigNames
import org.springframework.stereotype.Component

@Component
class ConfigNamesDataLoader(private val service: AuroraApiMetadataService) : KeyDataLoader<AuroraApiMetadata, ConfigNames> {
    override suspend fun getByKey(key: AuroraApiMetadata, context: GoboGraphQLContext) = service.getConfigNames()
}
