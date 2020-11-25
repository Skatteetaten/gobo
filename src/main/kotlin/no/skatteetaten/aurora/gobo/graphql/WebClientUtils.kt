package no.skatteetaten.aurora.gobo.graphql

import kotlinx.coroutines.reactive.awaitSingle
import mu.KotlinLogging
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.PrematureCloseException
import reactor.util.retry.Retry
import java.time.Duration

suspend inline fun <reified T : Any> WebClient.ResponseSpec.awaitWithRetry(
    min: Long = 100,
    max: Long = 1000,
    maxAttempts: Long = 3
): T = bodyToMono<T>()
    .retryWhen(
        Retry.backoff(maxAttempts, Duration.ofMillis(min))
            .maxBackoff(Duration.ofMillis(max))
            .filter { it is PrematureCloseException }
            .doBeforeRetry {
                KotlinLogging.logger {}.debug {
                    val e = it.failure()
                    "Retrying failed request times=${it.totalRetries()} errorType=${e.javaClass.simpleName} errorMessage=${e.message}"
                }
            }
    ).awaitSingle()
