package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.application.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import org.springframework.stereotype.Component

@Component
class ApplicationQueryResolver(val applicationService: ApplicationService) : GraphQLQueryResolver {

    fun getApplications(affiliations: List<String>): ApplicationsConnection {
        val applications = applicationService.getApplications(affiliations).map {
            ApplicationEdge(
                "", Application(
                    Affiliation(it.affiliation, ApplicationsConnection(emptyList(), 0, null)),
                    it.environment,
                    Namespace(
                        it.name,
                        Affiliation(it.affiliation, ApplicationsConnection(emptyList(), 0, null)),
                        ApplicationsConnection(emptyList(), 0, null)
                    ),
                    it.name,
                    Status(it.status.code, it.status.comment),
                    Version(it.version.deployTag, it.version.auroraVersion)
                )
            )
        }
        return ApplicationsConnection(applications, applications.size, null)
    }
}