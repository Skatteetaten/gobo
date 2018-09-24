package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentMutationResolver(private val applicationUpgradeService: ApplicationUpgradeService) :
    GraphQLMutationResolver {

    fun redeployWithVersion(input: ApplicationDeploymentVersionInput): Boolean {
        applicationUpgradeService.upgrade(input.applicationDeploymentId, input.version)
        return true
    }
}