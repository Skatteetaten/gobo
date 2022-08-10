package no.skatteetaten.aurora.gobo.integration.spotless

import no.skatteetaten.aurora.gobo.RequiresSpotless
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.cname.CnameAzure
import no.skatteetaten.aurora.gobo.integration.awaitWithRetry
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

interface SpotlessCnameService {
    suspend fun getCnameContent(affiliations: List<String>?): List<CnameAzure> = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Spotless integration is disabled for this environment")
}

@Service
@ConditionalOnBean(RequiresSpotless::class)
class SpotlessCnameServiceReactive(
    @TargetService(ServiceTypes.SPOTLESS) val webClient: WebClient,
    @Value("\${openshift.cluster}") val cluster: String,
) : SpotlessCnameService {
    override suspend fun getCnameContent(affiliations: List<String>?): List<CnameAzure> =
        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/api/v1/cname")
                    .queryParam("clusterId", cluster)
                    .queryParam("affiliations", affiliations)
                    .build()
            }
            .retrieve()
            .awaitWithRetry()
}

@Service
@ConditionalOnMissingBean(RequiresSpotless::class)
class SpotlessCnameServiceDisabled : SpotlessCnameService
