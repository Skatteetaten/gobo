package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import org.springframework.stereotype.Component
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService

data class AuroraConfigKey(
    val name: String,
    val refInput: String = "master",
)

@Component
class AuroraConfigDataLoader(private val auroraConfigService: AuroraConfigService) :
    KeyDataLoader<AuroraConfigKey, AuroraConfig> {

    override suspend fun getByKey(key: AuroraConfigKey, context: GoboGraphQLContext): AuroraConfig {
        return auroraConfigService.getAuroraConfig(context.token(), key.name, key.refInput)
    }
}
