package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import org.springframework.stereotype.Component
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService

data class AuroraConfigKey(
    val name: String,
    val refInput: String?,
)

@Component
class AuroraConfigDataLoader(private val service: AuroraConfigService) :
    KeyDataLoader<AuroraConfigKey, AuroraConfig> {

    override suspend fun getByKey(key: AuroraConfigKey, context: GoboGraphQLContext): AuroraConfig {
        return service.getAuroraConfig(context.token(), key.name, key.refInput ?: "master")
    }
}
