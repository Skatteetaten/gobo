package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
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
import org.springframework.web.reactive.function.client.awaitBody

inline fun <reified T : Any> Response<T>.responses(): List<T> = when {
    !this.success -> throw SourceSystemException(
        message = this.message,
        errorMessage = this.items.toString(),
        sourceSystem = "boober",
        extensions = mapOf("message" to this.message, "errors" to this.items)
    )
    this.count == 0 -> emptyList()
    else -> this.items.map { item ->
        kotlin.runCatching {
            jacksonObjectMapper().convertValue(item, T::class.java)
        }.onFailure {
            KotlinLogging.logger { }.error(it) { "Unable to parse response items from boober: $item" }
            throw it
        }.getOrThrow()
    }
}

inline fun <reified T : Any> Response<T>.response(): T = this.responses().first()

inline fun <reified T : Any> Response<T>.responseOrNull(): T? = this.responses().ifEmpty { null }?.first()

@Service
class BooberWebClient(
    @Value("\${integrations.boober.url:}") val booberUrl: String?,
    @TargetService(ServiceTypes.BOOBER) val webClient: WebClient,
    val objectMapper: ObjectMapper
) : WebClient by webClient {

    fun WebClient.RequestHeadersUriSpec<*>.booberUrl(
        url: String,
        params: Map<String, String> = emptyMap()
    ): WebClient.RequestHeadersSpec<*> =
        this.uri(getBooberUrl(url), params)

    fun WebClient.RequestBodyUriSpec.booberUrl(url: String, params: Map<String, String> = emptyMap()) =
        this.uri(getBooberUrl(url), params)

    final suspend inline fun <reified T : Any> WebClient.RequestHeadersSpec<*>.execute(
        token: String? = null,
        etag: String? = null
    ): Response<T> {
        return this.let {
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
            .awaitBody()
    }

    final suspend inline fun <reified T : Any> get(
        url: String,
        token: String? = null,
        etag: String? = null,
        params: Map<String, String> = emptyMap()
    ) =
        webClient.get().booberUrl(url, params).execute<T>(token = token, etag = etag)

    final suspend inline fun <reified T : Any> patch(
        url: String,
        body: Any,
        token: String? = null,
        params: Map<String, String> = emptyMap()
    ) =
        webClient.patch().booberUrl(url, params).body(BodyInserters.fromValue(body)).execute<T>(token)

    final suspend inline fun <reified T : Any> put(
        url: String,
        body: Any,
        token: String? = null,
        etag: String? = null,
        params: Map<String, String> = emptyMap()
    ) =
        webClient.put().booberUrl(url, params).body(BodyInserters.fromValue(body))
            .execute<T>(token = token, etag = etag)

    final suspend inline fun <reified T : Any> post(
        url: String,
        body: Any,
        token: String? = null,
        params: Map<String, String> = emptyMap()
    ) =
        webClient.post().booberUrl(url, params).body(BodyInserters.fromValue(body)).execute<T>(token)

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
}
