package no.skatteetaten.aurora.gobo.graphql.application

import no.skatteetaten.aurora.gobo.KeysBatchDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap

@Component
class ApplicationBatchDataLoader(val applicationService: ApplicationService) :
    KeysBatchDataLoader<String, List<Application>> {

    override suspend fun getByKeys(keys: Set<String>, context: GoboGraphQLContext): Map<String, List<Application>> {
        val applications = applicationService.getApplications(keys.toList())
        val results = LinkedMultiValueMap<String, Application>()
        applications.forEach {
            results.add(
                it.applicationDeployments.first().affiliation,
                Application(it.identifier, it.name, it.applicationDeployments.map { ApplicationDeployment.create(it) })
            )
        }

        return results
    }
}
