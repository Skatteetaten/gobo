package no.skatteetaten.aurora.gobo.graphql.auroraapimetadata

import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.AuroraApiMetadataService
import org.springframework.stereotype.Component

@Component
class ConfigNamesDataLoader(private val service: AuroraApiMetadataService) : GoboDataLoader<AuroraApiMetadata, List<String>>() {
    override suspend fun getByKeys(keys: Set<AuroraApiMetadata>, ctx: GoboGraphQLContext): Map<AuroraApiMetadata, List<String>> {
        return keys.associateWith { service.getConfigNames().names }
    }
}
