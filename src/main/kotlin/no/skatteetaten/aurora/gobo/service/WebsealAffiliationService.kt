package no.skatteetaten.aurora.gobo.service

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.skap.WebsealServiceBlocking
import no.skatteetaten.aurora.gobo.integration.skap.WebsealState
import org.springframework.stereotype.Service

@Service
class WebsealAffiliationService(
    private val applicationService: ApplicationServiceBlocking,
    private val websealService: WebsealServiceBlocking
) {
    fun getWebsealState(affiliations: List<String>): Map<String, List<WebsealState>> {
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