package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.application.createApplicationEdges
import org.springframework.stereotype.Component

@Component
class ApplicationDeploymentQueryResolver(private val applicationService: ApplicationService) : GraphQLQueryResolver {

    fun getApplicationDeployment(id: String): ApplicationDeployment? {

        val applicationDeploymentDetails = applicationService.getApplicationDeploymentDetails(id).block()!!
        val app = applicationDeploymentDetails._embedded?.get("Application")!!

        val applicationEdges = createApplicationEdges(listOf(app), listOf(applicationDeploymentDetails))
        return applicationEdges.first().node.applicationDeployments.first()
    }
}