package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLMutationResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.security.UserService
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentMutationResolver(
    private val userService: UserService,
    private val applicationUpgradeService: ApplicationUpgradeService
) :
    GraphQLMutationResolver {

    fun redeployWithVersion(input: ApplicationDeploymentVersionInput, dfe: DataFetchingEnvironment): Boolean {
        val token = userService.getCurrentUser(dfe).token
        applicationUpgradeService.upgrade(input.applicationDeploymentId, input.version, token).block()
        return true
    }

    fun refreshApplicationDeployment(input: ApplicationDeploymentRefreshInput, dfe: DataFetchingEnvironment): String {
        val token = userService.getCurrentUser(dfe).token
        return applicationUpgradeService.refreshApplicationDeployment(input.applicationDeploymentId, token)
    }
}