package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import org.springframework.stereotype.Component
import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService

@Component
class AuroraConfigFileResourceDataLoader(private val auroraConfigService: AuroraConfigService) :
    GoboDataLoader<ApplicationDeployment, List<AuroraConfigFileResource>>() {
    override suspend fun getByKeys(
        keys: Set<ApplicationDeployment>,
        ctx: GoboGraphQLContext
    ): Map<ApplicationDeployment, List<AuroraConfigFileResource>> {
        return keys.associateWith {
            auroraConfigService.getAuroraConfigFiles(
                token = ctx.token(),
                it.affiliation.name,
                it.environment,
                it.name
            )
        }
    }
}
