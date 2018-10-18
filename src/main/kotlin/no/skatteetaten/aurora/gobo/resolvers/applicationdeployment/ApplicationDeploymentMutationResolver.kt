package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import no.skatteetaten.aurora.gobo.integration.mokey.RefreshParams
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentMutationResolver(private val applicationUpgradeService: ApplicationUpgradeService) :
    GraphQLMutationResolver {

    fun redeployWithVersion(input: ApplicationDeploymentVersionInput): Boolean {
        applicationUpgradeService.upgrade(input.applicationDeploymentId, input.version).block()
        return true
    }

    fun refreshApplicationDeployments(input: ApplicationDeploymentRefreshInput): String {
        val refreshParams = RefreshParams(input.applicationDeploymentId, input.affiliations)
        applicationUpgradeService.refreshApplicationDeployments(refreshParams)

        return input.applicationDeploymentId ?: input.affiliations?.joinToString { "$it " }
        ?: throw IllegalArgumentException("Nothing to refresh")
    }
}