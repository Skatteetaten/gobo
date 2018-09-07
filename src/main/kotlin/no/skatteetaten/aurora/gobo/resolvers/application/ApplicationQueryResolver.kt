package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationQueryResolver(private val applicationService: ApplicationService) : GraphQLQueryResolver {

    fun getApplications(
        affiliations: List<String>,
        applications: List<String>? = null
    ): ApplicationsConnection {

        // TODO: When applications is set, limit the amount of data collected for ApplicationDeploymentDetails.
        val details = applicationService.getApplicationDeploymentDetails(affiliations)
        val applicationResources = applicationService.getApplications(affiliations, applications)
        val applicationEdges = createApplicationEdges(applicationResources, details)

        return ApplicationsConnection(applicationEdges)
    }
}