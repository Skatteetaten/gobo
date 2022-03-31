package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import org.springframework.stereotype.Component
import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService

data class ApplicationFilesKey(
    val configName: String,
    val configRef: String,
    val types: List<AuroraConfigFileType>?,
    val applicationDeploymentRef: List<ApplicationDeploymentRef>
)

@Component
class ApplicationFilesResourceDataLoader(
    private val auroraConfigService: AuroraConfigService
) :
    GoboDataLoader<ApplicationFilesKey, List<ApplicationFilesResource>>() {
    override suspend fun getByKeys(
        keys: Set<ApplicationFilesKey>,
        ctx: GraphQLContext
    ): Map<ApplicationFilesKey, List<ApplicationFilesResource>> {
        return keys.associateWith { files ->
            files.applicationDeploymentRef.map { ref ->
                ApplicationFilesResource(
                    files = auroraConfigService.getAuroraConfigFiles(
                        token = ctx.token,
                        files.configName,
                        ref.environment,
                        ref.application,
                        files.configRef
                    ).filter {
                        files.types == null || files.types.contains(it.type)
                    },
                    applicationDeploymentRef = ref
                )
            }
        }
    }
}
