package no.skatteetaten.aurora.gobo.graphql.application

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.KeysBatchDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class ApplicationBatchDataLoader(val applicationService: ApplicationService) :
    KeysBatchDataLoader<String, List<Application>> {

    override suspend fun getByKeys(
        affiliations: Set<String>,
        context: GoboGraphQLContext
    ): Map<String, List<Application>> {
        val applications = applicationService.getApplications(affiliations.toList())
        return affiliations.map { affiliation ->
            val apps = applications.filter { it.applicationDeployments.first().affiliation == affiliation }.map { app ->
                Application(
                    id = app.identifier,
                    name = app.name,
                    applicationDeployments = app.applicationDeployments.map { ApplicationDeployment.create(it) }
                )
            }
            if (apps.isEmpty()) {
                logger.warn("Empty application list for $affiliation")
            }

            affiliation to apps
        }.toMap()
    }
}
