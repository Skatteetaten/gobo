package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import org.springframework.stereotype.Component

data class AuroraConfigKey(
    val name: String,
    val refInput: String = "master",
)

@Component
class AuroraConfigDataLoader(private val auroraConfigService: AuroraConfigService) : GoboDataLoader<AuroraConfigKey, AuroraConfig>() {
    override suspend fun getByKeys(keys: Set<AuroraConfigKey>, ctx: GraphQLContext): Map<AuroraConfigKey, AuroraConfig> {
        return keys.associateWith { auroraConfigService.getAuroraConfig(ctx.token, it.name, it.refInput) }
    }
}
