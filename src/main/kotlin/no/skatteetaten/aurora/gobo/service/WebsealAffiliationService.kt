package no.skatteetaten.aurora.gobo.service

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.skap.WebsealService
import no.skatteetaten.aurora.gobo.integration.skap.WebsealStateResource
import org.springframework.stereotype.Service

@Service
class WebsealAffiliationService(
    private val applicationService: ApplicationService,
    private val websealService: WebsealService
) {
    suspend fun getWebsealState(affiliations: List<String>): Map<String, List<WebsealStateResource>> {
        val applicationDeployments = applicationService
            .getApplications(affiliations = affiliations)
            .flatMap { it.applicationDeployments }
        val websealStates = websealService.getStates()

        return applicationDeployments.groupBy { it.affiliation }
            .map { entry ->
                entry.key to websealStates.filter { websealState ->
                    val namespaces = entry.value.map { it.namespace }
                    namespaces.contains(websealState.namespace)
                }
            }.toMap()
    }
}
