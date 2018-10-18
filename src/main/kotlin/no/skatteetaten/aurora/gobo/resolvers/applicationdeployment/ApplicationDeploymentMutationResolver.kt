package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.security.currentUser
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentMutationResolver(
    private val applicationUpgradeService: ApplicationUpgradeService
) :
    GraphQLMutationResolver {

    fun redeployWithVersion(input: ApplicationDeploymentVersionInput, dfe: DataFetchingEnvironment): Boolean {
        applicationUpgradeService.upgrade(input.applicationDeploymentId, input.version, dfe.currentUser().token).block()
        return true
    }

    fun refreshApplicationDeployment(input: ApplicationDeploymentRefreshInput, dfe: DataFetchingEnvironment) =
        applicationUpgradeService.refreshApplicationDeployment(input.applicationDeploymentId, dfe.currentUser().token)
}