package no.skatteetaten.aurora.gobo.graphql.application

import com.expediagroup.graphql.server.operations.Query
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationQuery(private val applicationService: ApplicationService) : Query {

    suspend fun applications(
        affiliations: List<String>,
        applications: List<String>? = null
    ): ApplicationsConnection {
        val applicationResources = applicationService.getApplications(affiliations, applications)
        val applicationEdges = createApplicationEdges(applicationResources)

        return ApplicationsConnection(applicationEdges)
    }
}
