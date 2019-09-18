package no.skatteetaten.aurora.gobo.resolvers

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import graphql.schema.DataFetchingEnvironment
import graphql.servlet.context.GraphQLServletContext
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
        val authorization = this.getContext<GraphQLServletContext>().httpServletRequest.getHeader("Authorization")
        return authorization?.split(" ")?.lastOrNull()?.trim()
    }

fun <T : KeyDataLoader<*, V>, V> DataFetchingEnvironment.loader(type: KClass<T>): DataLoader<Any, Try<V>> {
    val key = "${type.simpleName}"
    return this.getContext<GraphQLServletContext>().dataLoaderRegistry.get()
        .getDataLoader<Any, Try<V>>(key) ?: throw IllegalStateException("No $key found")
}

fun <T : MultipleKeysDataLoader<*, V>, V> DataFetchingEnvironment.multipleKeysLoader(type: KClass<T>): DataLoader<Any, V> {
    val key = "${type.simpleName}"
    return this.getContext<GraphQLServletContext>().dataLoaderRegistry.get()
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
            else -> throw SourceSystemException(message = it.message ?: "", cause = it, errorMessage = "Error response")
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
