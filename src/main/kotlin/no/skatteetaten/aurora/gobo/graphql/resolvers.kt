package no.skatteetaten.aurora.gobo.graphql

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import io.netty.handler.timeout.ReadTimeoutException
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.netty.http.client.PrematureCloseException
import reactor.retry.RetryContext

private val logger = KotlinLogging.logger { }

fun <T> RetryContext<Mono<T>>.isServerError() =
    this.exception() is WebClientResponseException && (this.exception() as WebClientResponseException).statusCode.is5xxServerError

fun <T> RetryContext<Mono<T>>.isTimeout() =
    this.exception() is PrematureCloseException || this.exception() is ReadTimeoutException

fun <T> Mono<T>.handleError(sourceSystem: String?) =
    this.doOnError {
        when (it) {
            is WebClientResponseException -> {
                val errorMessage = "Error in response, status=${it.rawStatusCode} message=${it.statusText}"
                val message = it.readResponse() ?: errorMessage

                throw SourceSystemException(
                    message = message,
                    errorMessage = errorMessage,
                    cause = it,
                    sourceSystem = sourceSystem,
                    code = it.statusCode.name
                )
            }
            is SourceSystemException -> throw it
            else -> throw SourceSystemException(
                message = it.message
                    ?: "",
                cause = it,
                errorMessage = "Error response"
            )
        }
    }

private fun WebClientResponseException.readResponse(): String? {
    this.request?.let {
        logger.info { "Error request url:${it.uri.toASCIIString()}" }
    }

    val body = this.responseBodyAsString
    logger.debug { "Error response body: $body" }

    val json = JsonPath.parse(body, Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
    return json.read<String>("$.message") ?: json.read<String>("$.items[0]")
}
