package no.skatteetaten.aurora.gobo.application

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
class ApplicationQueryResolver(val applicationService: ApplicationService) : GraphQLQueryResolver {

    fun getApplications(affiliations: List<String>): List<Application> =
        applicationService.getApplications(affiliations)
}