package no.skatteetaten.aurora.gobo.integration.skap

import java.time.Duration
import no.skatteetaten.aurora.gobo.RequiresSkap
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.resolvers.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.resolvers.blockNonNullAndHandleError
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
@ConditionalOnBean(RequiresSkap::class)
class RouteServiceReactive(
    private val sharedSecretReader: SharedSecretReader,
    @TargetService(ServiceTypes.SKAP) private val webClient: WebClient
) {
    fun getProgressions(namespace: String, name: String): Mono<List<Progression>> =
        webClient
            .get()
            .uri {
                it.path("/job/list").queryParam("namespace", namespace).queryParam("name", name).build()
            }
            .header(HttpHeaders.AUTHORIZATION, "$HEADER_AURORA_TOKEN ${sharedSecretReader.secret}")
            .retrieve()
            .bodyToMono()
}

interface RouteService {
    fun getProgressions(namespace: String, name: String): List<Progression> = integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw IntegrationDisabledException("Skap integration is disabled for this environment")
}

@Service
@ConditionalOnBean(RequiresSkap::class)
class RouteServiceBlocking(private val routeService: RouteServiceReactive) : RouteService {
    override fun getProgressions(namespace: String, name: String) =
        routeService.getProgressions(namespace, name).blockNonNullWithTimeout()

    fun <T> Mono<T>.blockNonNullWithTimeout() = this.blockNonNullAndHandleError(Duration.ofSeconds(30), "skap")
}

@Service
@ConditionalOnMissingBean(RequiresSkap::class)
class RouteServiceDisabled : RouteService
