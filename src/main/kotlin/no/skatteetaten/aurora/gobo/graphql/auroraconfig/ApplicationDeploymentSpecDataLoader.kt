package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import org.springframework.stereotype.Component

data class AdSpecKey(
    val configName: String,
    val configRef: String,
    val applicationDeploymentRef: List<ApplicationDeploymentRef>
)

@Component
class ApplicationDeploymentSpecDataLoader(private val applicationDeploymentService: ApplicationDeploymentService) :
    GoboDataLoader<AdSpecKey, List<ApplicationDeploymentSpec>>() {
    override suspend fun getByKeys(keys: Set<AdSpecKey>, ctx: GraphQLContext): Map<AdSpecKey, List<ApplicationDeploymentSpec>> {
        return keys.associateWith {
            applicationDeploymentService.getSpec(
                token = ctx.token,
                auroraConfigName = it.configName,
                auroraConfigReference = it.configRef,
                applicationDeploymentReferenceList = it.applicationDeploymentRef
            )
        }
    }
}
