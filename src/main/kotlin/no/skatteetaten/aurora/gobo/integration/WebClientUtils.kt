package no.skatteetaten.aurora.gobo.integration

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactive.awaitSingle
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.boober.objectMapper
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.netty.http.client.PrematureCloseException
import reactor.util.retry.Retry
import java.time.Duration

val webclientLogger = KotlinLogging.logger {}

inline fun <reified T : Throwable> WebClient.ResponseSpec.onStatusNotFound(crossinline fn: (status: HttpStatus, body: String) -> T): WebClient.ResponseSpec =
    this.onStatus({ it == HttpStatus.NOT_FOUND }) {
        it.bodyToMono<String>().defaultIfEmpty("").flatMap { body ->
            Mono.error(fn(it.statusCode(), body))
        }
    }

inline fun <reified T : Throwable> WebClient.ResponseSpec.onStatusNotOk(crossinline fn: (status: HttpStatus, body: String) -> T): WebClient.ResponseSpec =
    this.onStatus({ it != HttpStatus.OK }) {
        it.bodyToMono<String>().defaultIfEmpty("").flatMap { body ->
            Mono.error(fn(it.statusCode(), body))
        }
    }

suspend inline fun <reified T : Any> WebClient.ResponseSpec.awaitWithRetry(
    min: Long = 100,
    max: Long = 1000,
    maxAttempts: Long = 3
): T = bodyToMono<T>()
    .retryWhen(
        Retry.backoff(maxAttempts, Duration.ofMillis(min))
            .maxBackoff(Duration.ofMillis(max))
            .filter {
                ExceptionUtils.throwableOfType(it, PrematureCloseException::class.java)?.let { true }
                    ?: false
            }
            .doBeforeRetry {
                KotlinLogging.logger {}.debug {
                    val e = it.failure()
                    "Retrying failed request times=${it.totalRetries()} errorType=${e.javaClass.simpleName} errorMessage=${e.message}"
                }
            }
    ).awaitSingle()

suspend inline fun <reified T : Any> WebClient.postOrNull(
    body: Any,
    uri: String,
    vararg uriVariables: String
): T? =
    runCatching {
        post<T>(uri, body, *uriVariables)
    }.onFailure {
        val additionalErrorMessage = when (it) {
            is WebClientResponseException -> {
                val response = runCatching {
                    objectMapper.readValue<T>(it.responseBodyAsByteArray)
                }.getOrNull() ?: "Could not read response body. Cause=${it.cause}"

                "statusCode=${it.statusCode} response=$response"
            }
            else -> "unknown reason. Exception of type ${it::class.simpleName} with message=${it.message}"
        }
        webclientLogger.warn(it) { "Request failed for url=$uri $additionalErrorMessage" }
    }.getOrNull()

suspend inline fun <reified T : Any> WebClient.post(
    uri: String,
    body: Any,
    vararg uriVariables: String
) = this.post()
    .uri(uri, *uriVariables)
    .body(BodyInserters.fromValue(body))
    .retrieve()
    .awaitWithRetry<T>()
