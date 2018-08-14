package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.service.application.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationQueryResolver(private val applicationService: ApplicationService) : GraphQLQueryResolver {

    fun getApplications(
        affiliations: List<String>,
        applications: List<String>? = null
    ): ApplicationsConnection {
        val details = applicationService.getApplicationInstanceDetails(affiliations)
        val applicationEdges = applicationService
            .getApplications(affiliations, applications)
            .map { createApplicationEdge(it, details) }

        return ApplicationsConnection(applicationEdges, null)
    }
}