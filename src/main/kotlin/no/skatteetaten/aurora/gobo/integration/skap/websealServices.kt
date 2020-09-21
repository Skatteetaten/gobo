package no.skatteetaten.aurora.gobo.integration.skap

import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.gobo.RequiresSkap
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.resolvers.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
@ConditionalOnBean(RequiresSkap::class)
class WebsealServiceReactive(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.SKAP) private val webClient: WebClient
) {

    suspend fun getStates(): List<WebsealStateResource> =
        webClient
            .get()
            .uri("/webseal/v3")
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .awaitBody()
}

interface WebsealService {
    fun getStates(): List<WebsealStateResource> = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Skap integration is disabled for this environment")
}

@Service
@ConditionalOnBean(RequiresSkap::class)
class WebsealServiceBlocking(private val websealService: WebsealServiceReactive) : WebsealService {
    override fun getStates() = runBlocking { websealService.getStates() }
}

@Service
@ConditionalOnMissingBean(RequiresSkap::class)
class WebsealServiceDisabled : WebsealService
