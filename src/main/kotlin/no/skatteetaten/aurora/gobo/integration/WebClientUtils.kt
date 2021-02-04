package no.skatteetaten.aurora.gobo.integration

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactive.awaitSingle
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.boober.objectMapper
import org.apache.commons.lang3.exception.ExceptionUtils
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.PrematureCloseException
import reactor.util.retry.Retry
import java.time.Duration

val webclientLogger = KotlinLogging.logger {}

suspend inline fun <reified T : Any> WebClient.ResponseSpec.awaitWithRetry(
    min: Long = 100,
    max: Long = 1000,
    maxAttempts: Long = 3
): T = bodyToMono<T>()
    .retryWhen(
        Retry.backoff(maxAttempts, Duration.ofMillis(min))
            .maxBackoff(Duration.ofMillis(max))
            .filter { ExceptionUtils.throwableOfType(it, PrematureCloseException::class.java)?.let { true } ?: false }
            .doBeforeRetry {
                KotlinLogging.logger {}.debug {
                    val e = it.failure()
                    "Retrying failed request times=${it.totalRetries()} errorType=${e.javaClass.simpleName} errorMessage=${e.message}"
                }
            }
    ).awaitSingle()

suspend inline fun <reified T : Any, R> WebClient.postOrNull(
    body: R,
    uri: String,
    vararg uriVariables: String
): T? =
    runCatching {
        post<T, R>(uri, body, *uriVariables)
    }.onFailure {
        val additionalErrorMessage = when (it) {
            is WebClientResponseException -> {
                val body = objectMapper.readValue<T>(it.responseBodyAsByteArray)
                "statusCode=${it.statusCode} response=$body"
            }
            else -> "unknown reason. Exception of type ${it::class.simpleName} with message=${it.message}"
        }
        webclientLogger.warn(it) { "Request failed for url=$uri $additionalErrorMessage" }
    }.getOrNull()

suspend inline fun <reified T : Any, R> WebClient.post(
    uri: String,
    body: R,
    vararg uriVariables: String
) = this.post()
    .uri(uri, *uriVariables)
    .body(BodyInserters.fromValue(body))
    .retrieve()
    .awaitWithRetry<T>()
