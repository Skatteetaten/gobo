package no.skatteetaten.aurora.gobo.resolvers

import graphql.schema.DataFetchingEnvironment
import graphql.servlet.GraphQLContext
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

fun <T> Mono<T>.blockNonNullAndHandleError(duration: Duration = Duration.ofSeconds(30)) =
    this.switchIfEmpty(SourceSystemException("Empty response").toMono())
        .blockAndHandleError(duration)!!

fun <T> Mono<T>.blockAndHandleError(duration: Duration = Duration.ofSeconds(30)) =
    this.handleError()
        .block(duration)

fun <T> Mono<T>.handleError() =
    this.doOnError {
        if (it is WebClientResponseException) {
            throw SourceSystemException(
                "Error in response, status:${it.statusCode} message:${it.statusText}",
                it,
                it.responseBodyAsString
            )
        }
        throw SourceSystemException("Error response", it)
    }
