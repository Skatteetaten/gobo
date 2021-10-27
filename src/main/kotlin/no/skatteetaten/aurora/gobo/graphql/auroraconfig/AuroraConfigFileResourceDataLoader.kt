package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails.ApplicationDeploymentDetails
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class AuroraConfigFileResourceDataLoader(
    private val auroraConfigService: AuroraConfigService,
    private val applicationService: ApplicationService
) :
    GoboDataLoader<String, List<AuroraConfigFileResource>>() {
    override suspend fun getByKeys(
        keys: Set<String>,
        ctx: GoboGraphQLContext
    ): Map<String, List<AuroraConfigFileResource>> {

        return keys.associateWith {
            val details =
                ApplicationDeploymentDetails.create(applicationService.getApplicationDeploymentDetails(ctx.token(), it))

            auroraConfigService.getAuroraConfigFiles(
                token = ctx.token(),
                details.applicationDeploymentCommand.auroraConfig.name,
                details.applicationDeploymentCommand.applicationDeploymentRef.environment,
                details.applicationDeploymentCommand.applicationDeploymentRef.application
            )
        }
    }
}
