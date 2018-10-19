package no.skatteetaten.aurora.gobo.resolvers

import graphql.schema.DataFetchingEnvironment
import graphql.servlet.GraphQLContext
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import org.dataloader.DataLoader
import org.dataloader.Try
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

fun <T : Any> DataFetchingEnvironment.loader(type: KClass<T>): DataLoader<Any, Try<T>> {
    val key = "${type.simpleName}DataLoader"
    return this.getContext<GraphQLContext>().dataLoaderRegistry.get()
        .getDataLoader<Any, Try<T>>(key) ?: throw IllegalStateException("No $key found")
}

fun <T> Mono<T>.blockNonNull() =
    this.switchIfEmpty(SourceSystemException("Empty response").toMono())
        .doOnError { throw SourceSystemException("Error response", it) }
        .block(Duration.ofSeconds(30))!!
