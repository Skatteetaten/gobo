package no.skatteetaten.aurora.gobo.resolvers.auroraconfig

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.resolvers.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeploymentRef
import org.springframework.stereotype.Component

data class AdSpecKey(val configName: String, val configRef: String, val applicationDeploymentRef: List<ApplicationDeploymentRef>)

@Component
class ApplicationDeploymentSpecListDataLoader(
    private val applicationDeploymentService: ApplicationDeploymentService
) : KeyDataLoader<AdSpecKey, List<ApplicationDeploymentSpec>> {

    override suspend fun getByKey(key: AdSpecKey, context: GoboGraphQLContext): List<ApplicationDeploymentSpec> {
        return applicationDeploymentService.getSpec(
            context.token!!,
            key.configName,
            key.configRef,
            key.applicationDeploymentRef
        )
    }
}
