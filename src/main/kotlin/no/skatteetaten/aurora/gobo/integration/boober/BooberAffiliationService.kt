package no.skatteetaten.aurora.gobo.integration.boober

import org.springframework.stereotype.Service

@Service
class BooberAffiliationService(private val booberWebClient: BooberWebClient) {

    suspend fun getAllAffiliationNames(): List<String> =
        booberWebClient.get<String>("/v1/auroraconfignames").responses()
}
