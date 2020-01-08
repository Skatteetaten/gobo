package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
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

    final inline fun <reified T : Any> anonymousGet(url: String, params: List<String> = emptyList()): Flux<T> =
        execute {
            it.get().uri(getBooberUrl(url), params)
        }

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
            it.patch().uri(getBooberUrl(url), params).body(BodyInserters.fromValue(body))
        }

    final inline fun <reified T : Any> put(
        token: String,
        url: String,
        params: List<String> = emptyList(),
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
    final inline fun <reified T : Any> execute(
        token: String? = null,
        fn: (WebClient) -> WebClient.RequestHeadersSpec<*>
    ): Flux<T> {
        val response: Mono<Response<T>> = fn(webClient).let {
            if(token != null) {
                it.header(HttpHeaders.AUTHORIZATION, "Bearer $token")

            } else {
                it
            }
        }.retrieve().bodyToMono()



        return response.onErrorMap {
            val (message, code) = if (it is WebClientResponseException) {
                val responseObj = objectMapper.readValue<Response<Any>>(it.responseBodyAsString)
                Pair("message=${responseObj.message} items=${responseObj.items}", it.statusCode.value().toString())
            } else {
                Pair(it.message ?: "", "")
            }

            throw SourceSystemException(
                message = "Exception occurred in Boober integration.",
                errorMessage = "Response $message",
                code = code,
                sourceSystem = "boober"
            )
        }.flatMapMany { r ->
            if (!r.success) SourceSystemException(
                message = r.message,
                errorMessage = r.items.toString(),
                sourceSystem = "boober"
            ).toFlux()
            else if (r.count == 0) Flux.empty()
            else r.items.map { item -> objectMapper.convertValue(item, T::class.java) }.toFlux()
        }
    }
}
