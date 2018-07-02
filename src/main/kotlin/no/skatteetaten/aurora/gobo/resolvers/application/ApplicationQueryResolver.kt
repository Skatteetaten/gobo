package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.application.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationQueryResolver(private val applicationService: ApplicationService) : GraphQLQueryResolver {

    fun getApplications(affiliations: List<String>): ApplicationsConnection {
        val details = applicationService.getApplicationInstanceDetails(affiliations)
        val applications = applicationService
            .getApplications(affiliations)
            .map { createApplicationEdge(it, details) }

        return ApplicationsConnection(applications, null)
    }
}