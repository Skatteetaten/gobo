package no.skatteetaten.aurora.gobo.resolvers.application

import com.expediagroup.graphql.spring.operations.Query
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationQuery(private val applicationService: ApplicationService) : Query {

    suspend fun applications(
        affiliations: List<String>,
        applications: List<String>?
    ): ApplicationsConnection {
        val applicationResources = applicationService.getApplications(affiliations, applications)
        val applicationEdges = createApplicationEdges(applicationResources)

        return ApplicationsConnection(applicationEdges)
    }
}

/*
@Component
class ApplicationQueryResolver(private val applicationService: ApplicationServiceBlocking) : GraphQLQueryResolver {

    fun getApplications(
        affiliations: List<String>,
        applications: List<String>? = null
    ): ApplicationsConnection {
        val applicationResources = applicationService.getApplications(affiliations, applications)
        val applicationEdges = createApplicationEdges(applicationResources)

        return ApplicationsConnection(applicationEdges)
    }
}
*/
