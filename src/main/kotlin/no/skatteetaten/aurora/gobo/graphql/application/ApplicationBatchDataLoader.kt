package no.skatteetaten.aurora.gobo.graphql.application

import no.skatteetaten.aurora.gobo.KeysBatchDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

@Component
class ApplicationBatchDataLoader(val applicationService: ApplicationService) :
    KeysBatchDataLoader<String, List<Application>> {

    override suspend fun getByKeys(
        affiliations: Set<String>,
        context: GoboGraphQLContext
    ): Map<String, List<Application>> {
        val applications = applicationService.getApplications(affiliations.toList())
        return affiliations.map { affiliation ->
            val apps = applications.filter { it.applicationDeployments.first().affiliation == affiliation }.map {
                Application(it.identifier, it.name, it.applicationDeployments.map { ApplicationDeployment.create(it) })
            }
            affiliation to apps
        }.toMap()
    }
}
