package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentRefResource
import no.skatteetaten.aurora.gobo.security.UserService
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux

@Service
class AuroraConfigService(
    @TargetService(ServiceTypes.BOOBER) val webClient: WebClient,
    val userService: UserService
) {

    final inline fun <reified T : Any> get(url: String, params: List<String> = emptyList()): Flux<T> =
        execute {
            it.get().uri(url, params)
        }

    final inline fun <reified T : Any> patch(url: String, params: List<String> = emptyList(), body: String): Flux<T> =
        execute {
            it.patch().uri(url, params).body(BodyInserters.fromObject(body))
        }

    final inline fun <reified T : Any> put(url: String, params: List<String> = emptyList(), body: Any): Flux<T> =
        execute {
            it.put().uri(url, params).body(BodyInserters.fromObject(body))
        }

    final inline fun <reified T : Any> execute(fn: (WebClient) -> WebClient.RequestHeadersSpec<*>): Flux<T> {
        return try {
            val bodyToMono: Mono<Response<T>> = fn(webClient)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${userService.getToken()}")
                .retrieve()
                .bodyToMono()
            bodyToMono.flatMapMany {
                if (!it.success) SourceSystemException(it.message).toFlux()
                else if (it.count == 0) Flux.empty()
                else it.items.toFlux()
            }
        } catch (e: WebClientResponseException) {
            SourceSystemException(
                "Failed to get application deployment details, status:${e.statusCode} message:${e.statusText}",
                e,
                e.statusText,
                "Failed to get application deployment details"
            ).toFlux()
        }
    }
}

data class Response<T>(
    val success: Boolean = true,
    val message: String = "OK",
    val items: List<T>,
    val count: Int = items.size
)

data class AuroraConfigFileResource(
    val name: String,
    val contents: String,
    val type: AuroraConfigFileType
)

enum class AuroraConfigFileType {
    DEFAULT,
    GLOBAL,
    GLOBAL_OVERRIDE,
    BASE,
    BASE_OVERRIDE,
    ENV,
    ENV_OVERRIDE,
    APP,
    APP_OVERRIDE
}

data class ApplyPayload(
    val applicationDeploymentRefs: List<ApplicationDeploymentRefResource> = emptyList(),
    val overrides: Map<String, String> = mapOf()
)
