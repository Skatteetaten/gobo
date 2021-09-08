package no.skatteetaten.aurora.gobo.graphql.application

import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationDataLoader(val applicationService: ApplicationService) :
    GoboDataLoader<String, List<Application>>() {

    override suspend fun getByKeys(affiliations: Set<String>, ctx: GoboGraphQLContext): Map<String, List<Application>> {
        val applications = applicationService.getApplications(affiliations.toList())
        return affiliations.associateWith { affiliation ->
            applications.filter { it.applicationDeployments.first().affiliation == affiliation }.map { app ->
                Application(
                    id = app.identifier,
                    name = app.name,
                    applicationDeployments = app.applicationDeployments.map { ApplicationDeployment.create(it) }
                )
            }
        }
    }
}
