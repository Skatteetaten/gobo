package no.skatteetaten.aurora.gobo.resolvers

import graphql.schema.DataFetchingEnvironment
import graphql.servlet.GraphQLContext
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.dataloader.DataLoader
import org.dataloader.Try
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.time.Duration
import kotlin.reflect.KClass

val DataFetchingEnvironment.token: String?
    get() {
        val context = this.executionContext.context
        return when (context) {
            is GraphQLContext -> {
                val authorization = context.httpServletRequest
                    .map { it.getHeader("Authorization") }
                    .orElse(null)
                authorization?.split(" ")?.lastOrNull()?.trim()
            }
            else -> null
        }
    }

fun <T : KeyDataLoader<*, V>, V> DataFetchingEnvironment.loader(type: KClass<T>): DataLoader<Any, Try<V>> {
    val key = "${type.simpleName}"
    return this.getContext<GraphQLContext>().dataLoaderRegistry.get()
        .getDataLoader<Any, Try<V>>(key) ?: throw IllegalStateException("No $key found")
}

fun <T : MultipleKeysDataLoader<*, V>, V> DataFetchingEnvironment.multipleKeysLoader(type: KClass<T>): DataLoader<Any, V> {
    val key = "${type.simpleName}"
    return this.getContext<GraphQLContext>().dataLoaderRegistry.get()
        .getDataLoader<Any, V>(key) ?: throw IllegalStateException("No $key found")
}

fun <T> Mono<T>.blockNonNullAndHandleError(duration: Duration = Duration.ofSeconds(30), sourceSystem: String? = null) =
    this.switchIfEmpty(SourceSystemException("Empty response", sourceSystem = sourceSystem).toMono())
        .blockAndHandleError(duration, sourceSystem)!!

fun <T> Mono<T>.blockAndHandleError(duration: Duration = Duration.ofSeconds(30), sourceSystem: String? = null) =
    this.handleError(sourceSystem)
        .block(duration)

private val logger = KotlinLogging.logger { }
fun <T> Mono<T>.handleError(sourceSystem: String?) =
    this.doOnError {
        when (it) {
            is WebClientResponseException -> {
                it.request?.let { request ->
                    logger.error { "Error request url:${request.uri.toASCIIString()}" }
                }
                logger.error { "Error response body: ${it.responseBodyAsString}" }
                throw SourceSystemException(
                    message = "Error in response, status=${it.rawStatusCode} message=${it.statusText}",
                    cause = it,
                    sourceSystem = sourceSystem,
                    code = it.statusCode.name
                )
            }
            is SourceSystemException -> throw it
            else -> throw SourceSystemException(message = it.message ?: "", cause = it, errorMessage = "Error response")
        }
    }
