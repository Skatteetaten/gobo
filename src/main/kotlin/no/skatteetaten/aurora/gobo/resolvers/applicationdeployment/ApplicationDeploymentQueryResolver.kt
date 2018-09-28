package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentQueryResolver(private val applicationService: ApplicationService) : GraphQLQueryResolver {

    fun getApplicationDeployment(id: String): ApplicationDeployment? =
        applicationService.getApplicationDeployment(id).block()?.let {
            ApplicationDeployment.create(it)
        }
}