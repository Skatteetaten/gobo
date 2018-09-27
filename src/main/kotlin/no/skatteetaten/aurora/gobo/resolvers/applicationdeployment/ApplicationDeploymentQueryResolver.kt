package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.application.createApplicationEdges
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentQueryResolver(private val applicationService: ApplicationService) : GraphQLQueryResolver {

    fun getApplicationDeployment(id: String): ApplicationDeployment? =
        applicationService.getApplicationDeploymentDetailsById(id).block()?.let { details ->
            details.embeddedApplication?.let { app ->
                val applicationEdges = createApplicationEdges(listOf(app), listOf(details))
                return applicationEdges.firstOrNull()?.node?.applicationDeployments?.firstOrNull()
            }
        }
}