package no.skatteetaten.aurora.gobo.integration.boober

import org.springframework.stereotype.Service

@Service
class VaultService(private val booberWebClient: BooberWebClient) {

    suspend fun getVaults(affiliation: String) =
        booberWebClient.get<String>("/v1/vault/$affiliation").responses().let {
            ConfigNames(it)
        }
}
