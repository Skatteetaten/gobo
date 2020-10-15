package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.graphql.AccessDeniedException
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import org.springframework.stereotype.Component

data class AdSpecKey(
    val configName: String,
    val configRef: String,
    val applicationDeploymentRef: List<ApplicationDeploymentRef>
)

@Component
class ApplicationDeploymentSpecListDataLoader(
    private val applicationDeploymentService: ApplicationDeploymentService
) : KeyDataLoader<AdSpecKey, List<ApplicationDeploymentSpec>> {

    override suspend fun getByKey(key: AdSpecKey, context: GoboGraphQLContext): List<ApplicationDeploymentSpec> =
        context.token?.let {
            applicationDeploymentService.getSpec(
                token = it,
                auroraConfigName = key.configName,
                auroraConfigReference = key.configRef,
                applicationDeploymentReferenceList = key.applicationDeploymentRef
            )
        } ?: throw AccessDeniedException("No anonymous access") // TODO error handling
}
