package no.skatteetaten.aurora.gobo.integration.boober

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.ServiceTypes
import no.skatteetaten.aurora.gobo.TargetService
import no.skatteetaten.aurora.gobo.integration.Response
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientFilterFunction
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriComponentsBuilder

val objectMapper: ObjectMapper = jacksonObjectMapper().registerModules(JavaTimeModule())

/**
 * Ignore success, do not throw BooberIntegrationException here if success = false
 */
inline fun <reified T : Any> Response<T>.responsesIgnoreStatus() = this.copy(success = true).responses()

inline fun <reified T : Any> Response<T>.responses(): List<T> = when {
    !this.success -> throw BooberIntegrationException(response = this)
    this.count == 0 -> emptyList()
    else -> parseResponseItems(items)
}

data class ResponsesAndErrors<T>(val items: List<T>, val errors: List<T>)

inline fun <reified T : Any> Response<T>.responsesWithErrors(): ResponsesAndErrors<T> = when {
    !this.success -> throw BooberIntegrationException(response = this)
    this.count == 0 -> ResponsesAndErrors(emptyList(), emptyList())
    else -> this.let {
        val items = parseResponseItems(it.items)
        val errors = parseResponseItems(it.errors)
        ResponsesAndErrors(items, errors)
    }
}

inline fun <reified T : Any> parseResponseItems(items: List<T>?) =
    items?.map { item ->
        runCatching {
            objectMapper.convertValue(item, T::class.java)
        }.onFailure { e ->
            KotlinLogging.logger {}.error(e) { "Unable to parse response items from boober:$item as object of type:${T::class.simpleName}" }
        }.getOrThrow()
    } ?: emptyList<T>()

inline fun <reified T : Any> Response<T>.response(): T = this.responses().first()

inline fun <reified T : Any> Response<T>.responseOrNull(): T? = this.responses().ifEmpty { null }?.first()

@Service
class BooberWebClient(
    @Value("\${integrations.boober.url:}") val booberUrl: String?,
    @TargetService(ServiceTypes.BOOBER) private val webClient: WebClient,
    val objectMapper: ObjectMapper,
    @Value("\${boober.metrics.enabled:}") val metricsEnabled: Boolean? = false
) {
    @Suppress("LeakingThis")
    val client = when (metricsEnabled) {
        true -> webClient
        else -> {
            webClient.mutate().filters { filters ->
                filters.find {
                    it is MetricsWebClientFilterFunction
                }?.let {
                    filters.remove(it)
                }
            }.build()
        }
    }

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
        }
            .retrieve()
            .onStatus({ it != HttpStatus.OK }) {
                it.bodyToMono<Response<*>>().flatMap { body ->
                    BooberIntegrationException(body, it.statusCode()).toErrorMono()
                }
            }.awaitBody()
    }

    final suspend inline fun <reified T : Any> get(
        url: String,
        token: String? = null,
        etag: String? = null,
        params: Map<String, String> = emptyMap()
    ) =
        client.get().booberUrl(url, params).execute<T>(token = token, etag = etag)

    final suspend inline fun <reified T : Any> patch(
        url: String,
        body: Any,
        token: String? = null,
        params: Map<String, String> = emptyMap()
    ) =
        client.patch().booberUrl(url, params).body(BodyInserters.fromValue(body)).execute<T>(token)

    final suspend inline fun <reified T : Any> put(
        url: String,
        body: Any,
        token: String? = null,
        etag: String? = null,
        params: Map<String, String> = emptyMap()
    ) =
        client.put().booberUrl(url, params).body(BodyInserters.fromValue(body))
            .execute<T>(token = token, etag = etag)

    final suspend inline fun <reified T : Any> post(
        url: String,
        body: Any,
        token: String? = null,
        params: Map<String, String> = emptyMap()
    ) =
        client.post().booberUrl(url, params).body(BodyInserters.fromValue(body)).execute<T>(token)

    final suspend inline fun <reified T : Any> delete(
        url: String,
        token: String? = null,
        params: Map<String, String> = emptyMap()
    ) =
        client.delete().booberUrl(url, params).execute<T>(token)

    fun getBooberUrl(link: String): String {
        if (booberUrl.isNullOrEmpty()) {
            return link
        }

        if (link.startsWith("/")) {
            return "$booberUrl$link"
        }

        val linkHost = UriComponentsBuilder.fromHttpUrl(link).build().host!!
        val booberUri = UriComponentsBuilder.fromHttpUrl(booberUrl ?: linkHost).build()
        return UriComponentsBuilder
            .fromHttpUrl(link)
            .host(booberUri.host)
            .port(booberUri.port)
            .build(false)
            .toUriString()
    }
}
