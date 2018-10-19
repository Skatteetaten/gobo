package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentQueryResolver(private val applicationService: ApplicationServiceBlocking) : GraphQLQueryResolver {

    fun getApplicationDeployment(id: String): ApplicationDeployment? =
        applicationService.getApplicationDeployment(id).let {
            ApplicationDeployment.create(it)
        }
}