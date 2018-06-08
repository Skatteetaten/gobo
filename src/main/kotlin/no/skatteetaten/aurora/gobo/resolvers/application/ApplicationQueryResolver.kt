package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.application.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationQueryResolver(val applicationService: ApplicationService) : GraphQLQueryResolver {

    fun getApplications(affiliations: List<String>): ApplicationsConnection {
        val applications = applicationService
            .getApplications(affiliations)
            .map { createApplicationEdge(it) }

        return ApplicationsConnection(applications, null)
    }
}