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
    fun getWebsealState(affiliation: String): List<WebsealState> {
        val applications = applicationService.getApplications(affiliations = listOf(affiliation))
        val namespaces = applications.map { it.applicationDeployments }.flatten().map { it.namespace }
        return websealService.getStates().filter { namespaces.contains(it.namespace) }
    }
}