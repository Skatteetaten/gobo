package no.skatteetaten.aurora.gobo.resolvers.application

import com.expediagroup.graphql.spring.operations.Query
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import org.springframework.stereotype.Component

@Component
class ApplicationQueryResolver(private val applicationService: ApplicationServiceBlocking) : Query {

    fun getApplications(
        affiliations: List<String>,
        applications: List<String>? = null
    ): ApplicationsConnection {
        val applicationResources = applicationService.getApplications(affiliations, applications)
        val applicationEdges = createApplicationEdges(applicationResources)

        return ApplicationsConnection(applicationEdges)
    }
}
