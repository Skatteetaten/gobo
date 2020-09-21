package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import java.net.URI
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Service
class BooberWebClient(
    @Value("\${integrations.boober.url:}") val booberUrl: String?,
    @TargetService(ServiceTypes.BOOBER) val webClient: WebClient,
    val objectMapper: ObjectMapper
) {

    final inline fun <reified T : Any> anonymousGet(url: String, params: Map<String, String> = emptyMap()): Flux<T> =
        execute {
            it.get().uri(getBooberUrl(url), params)
        }

    final inline fun <reified T : Any> get(
        token: String,
        url: String,
        params: Map<String, String> = emptyMap()
    ): Flux<T> =
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
            it.patch().uri(getBooberUrl(url), params).body(BodyInserters.fromValue(body))
        }

    final inline fun <reified T : Any> put(
        token: String,
        url: String,
        params: Map<String, String> = emptyMap(),
        body: Any
    ): Flux<T> =
        execute(token) {
            it.put().uri(getBooberUrl(url), params).body(BodyInserters.fromValue(body))
        }

    final inline fun <reified T : Any> post(
        token: String,
        url: String,
        params: List<String> = emptyList(),
        body: Any
    ): Flux<T> =
        execute(token) {
            it.post().uri(getBooberUrl(url), params).body(BodyInserters.fromValue(body))
        }

    fun getBooberUrl(link: String): String {
        if (booberUrl.isNullOrEmpty()) {
            return link
        }

        if (link.startsWith("/")) {
            return "$booberUrl$link"
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
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): Flux<T> {
        return execute(null, fn)
    }

    // TODO: Is this the correct way to abstract this?
    final inline fun <reified T : Any> executeMono(
        token: String? = null,
        etag: String? = null,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): Mono<T> {
        return fn(webClient).let {
            if (token != null) {
                it.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            } else {
                it
            }
        }.let {
            if (etag != null) {
                it.header(HttpHeaders.IF_MATCH, etag)
            } else {
                it
            }
        }.retrieve()
            .bodyToMono<T>()
            .onErrorMap { handleBooberHttpError(it) }
    }

    fun handleBooberHttpError(it: Throwable): SourceSystemException {
        return if (it is WebClientResponseException) {
            val responseObj = objectMapper.readValue<Response<Any>>(it.responseBodyAsString)
            val message = "message=${responseObj.message} items=${responseObj.items}"
            SourceSystemException(
                message = "Exception occurred in Boober integration.",
                errorMessage = "Response $message",
                code = it.statusCode.value().toString(),
                sourceSystem = "boober",
                extensions = mapOf("message" to responseObj.message, "errors" to responseObj.items)
            )
        } else {
            SourceSystemException(
                message = "Exception occurred in Boober integration.",
                errorMessage = "Response ${it.message}",
                code = "",
                sourceSystem = "boober"
            )
        }
    }

    final inline fun <reified T : Any> execute(
        token: String? = null,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): Flux<T> {
        return executeMono<Response<Any>>(token, null, fn).flatMapMany { r ->
            if (!r.success) SourceSystemException(
                message = r.message,
                errorMessage = r.items.toString(),
                sourceSystem = "boober",
                extensions = mapOf("message" to r.message, "errors" to r.items)
            ).toFlux()
            else if (r.count == 0) Flux.empty()
            else r.items.map { item -> objectMapper.convertValue(item, T::class.java) }.toFlux()
        }
    }
}
