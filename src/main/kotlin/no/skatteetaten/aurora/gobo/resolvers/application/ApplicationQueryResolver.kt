package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.application.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationQueryResolver(val applicationService: ApplicationService) : GraphQLQueryResolver {

    fun getApplications(affiliations: List<String>): List<Application> =
        applicationService.getApplications(affiliations).map {
            Application(
                it.affiliation,
                it.environment,
                it.name,
                Status(it.status.code, it.status.comment),
                Version(it.version.deployTag, it.version.auroraVersion)
            )
        }
}