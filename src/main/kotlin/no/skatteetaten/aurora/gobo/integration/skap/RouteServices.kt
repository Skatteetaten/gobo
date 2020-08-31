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
class RouteServiceReactive(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.SKAP) private val webClient: WebClient
) {
    suspend fun getSkapJobs(namespace: String, name: String): List<SkapJob> =
        webClient
            .get()
            .uri {
                it.path("/job/list").queryParam("namespace", namespace).queryParam("name", name).build()
            }
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .awaitBody()
}

interface RouteService {
    fun getSkapJobs(namespace: String, name: String): List<SkapJob> = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Skap integration is disabled for this environment")
}

@Service
@ConditionalOnBean(RequiresSkap::class)
class RouteServiceBlocking(private val routeService: RouteServiceReactive) : RouteService {
    override fun getSkapJobs(namespace: String, name: String) =
        runBlocking { routeService.getSkapJobs(namespace, name) }
}

@Service
@ConditionalOnMissingBean(RequiresSkap::class)
class RouteServiceDisabled : RouteService
