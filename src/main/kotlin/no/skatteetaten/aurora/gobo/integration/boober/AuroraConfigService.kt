package no.skatteetaten.aurora.gobo.integration.boober

import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.createObjectMapper
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentRefResource
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import java.net.URI

@Service
class AuroraConfigService(
    @Value("\${boober.url:}") val booberUrl: String?,
    @TargetService(ServiceTypes.BOOBER) val webClient: WebClient
) {

    final inline fun <reified T : Any> get(token: String, url: String, params: List<String> = emptyList()): Flux<T> =
        execute(token) {
            it.get().uri(getBooberUrl(url), params)
        }

    final inline fun <reified T : Any> patch(
        token: String,
        url: String,
        params: Map<String, String> = emptyMap(),
        body: Any
    ): Flux<T> =
        execute(token) {
            it.patch().uri(getBooberUrl(url), params).body(BodyInserters.fromObject(body))
        }

    final inline fun <reified T : Any> put(
        token: String,
        url: String,
        params: List<String> = emptyList(),
        body: Any
    ): Flux<T> =
        execute(token) {
            it.put().uri(getBooberUrl(url), params).body(BodyInserters.fromObject(body))
        }

    fun getBooberUrl(link: String): String {
        if (booberUrl.isNullOrEmpty()) {
            return link
        }

        val booberUri = URI(booberUrl)
        val linkUri = URI(link)
        return URI(
            booberUri.scheme,
            linkUri.userInfo,
            booberUri.host,
            booberUri.port,
            linkUri.path,
            linkUri.query,
            linkUri.fragment
        ).toString()
    }

    final inline fun <reified T : Any> execute(
        token: String,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): Flux<T> {
        val response: Mono<Response> = fn(webClient)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .retrieve()
            .onStatus(HttpStatus::isError) {
                it.bodyToMono<String>().defaultIfEmpty("").map { body ->
                    SourceSystemException(
                        message = "Failed to get application deployment details, status:${it.statusCode().value()} message:${it.statusCode().reasonPhrase}",
                        code = it.statusCode().value().toString(),
                        errorMessage = body
                    )
                }
            }
            .bodyToMono()
        return response.flatMapMany { r ->
            if (!r.success) SourceSystemException(r.message).toFlux()
            else if (r.count == 0) Flux.empty()
            else r.items.map { item -> createObjectMapper().convertValue(item, T::class.java) }.toFlux()
        }
    }
}

data class Response(
    val success: Boolean = true,
    val message: String = "OK",
    val items: List<Any>,
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
