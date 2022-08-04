package no.skatteetaten.aurora.gobo.integration.spotless

import no.skatteetaten.aurora.gobo.RequiresSpotless
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.cname.CnameAzure
import no.skatteetaten.aurora.gobo.integration.awaitWithRetry
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

interface SpotlessCnameService {
    suspend fun getCnameContent(): List<CnameAzure> = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Spotless integration is disabled for this environment")
}

@Service
@ConditionalOnBean(RequiresSpotless::class)
class SpotlessCnameServiceReactive(@TargetService(ServiceTypes.SPOTLESS) val webClient: WebClient) : SpotlessCnameService {
    override suspend fun getCnameContent(): List<CnameAzure> =
        webClient
            .get()
            .uri("/api/v1/cname")
            .retrieve()
            .awaitWithRetry()
}

@Service
@ConditionalOnMissingBean(RequiresSpotless::class)
class SpotlessCnameServiceDisabled : SpotlessCnameService
