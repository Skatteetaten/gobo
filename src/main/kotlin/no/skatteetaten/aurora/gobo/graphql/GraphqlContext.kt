package no.skatteetaten.aurora.gobo.graphql

import brave.baggage.BaggageField
import com.expediagroup.graphql.execution.GraphQLContext
import com.expediagroup.graphql.spring.execution.GraphQLContextFactory
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.ReactorContext
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.errorhandling.isInvalidToken
import no.skatteetaten.aurora.webflux.AuroraRequestParser
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import kotlin.coroutines.coroutineContext

fun DataFetchingEnvironment.token() = this.getContext<GoboGraphQLContext>().token()

class GoboGraphQLContext(
    private val token: String?,
    val request: ServerHttpRequest,
    val response: ServerHttpResponse,
    val securityContext: Mono<SecurityContext>,
    var query: String? = null
) : GraphQLContext {
    suspend fun securityContext(): SecurityContext = runCatching { securityContext.awaitFirst() }
        .recoverCatching {
            if (!it.isInvalidToken()) {
                logger.info(it) { "Unable to get the security context" }
            }

            throw AccessDeniedException("Invalid bearer token", it)
        }
        .getOrThrow()

    fun token() = token ?: throw AccessDeniedException("Token is not set")
    fun korrelasjonsid() =
        request.korrelasjonsid() ?: BaggageField.getByName(AuroraRequestParser.KORRELASJONSID_FIELD)?.value ?: ""

    fun klientid() = request.klientid()
}

private val logger = KotlinLogging.logger {}

@Component
class GoboGraphQLContextFactory : GraphQLContextFactory<GoboGraphQLContext> {

    @ExperimentalCoroutinesApi
    override suspend fun generateContext(request: ServerHttpRequest, response: ServerHttpResponse): GoboGraphQLContext {
        request.logHeaders()

        val reactorContext =
            coroutineContext[ReactorContext]?.context ?: throw RuntimeException("Reactor Context unavailable")
        val securityContext = reactorContext.getOrDefault<Mono<SecurityContext>>(
            SecurityContext::class.java,
            Mono.error(AccessDeniedException("Security Context unavailable"))
        )!!

        return GoboGraphQLContext(
            token = request.headers.getFirst(HttpHeaders.AUTHORIZATION)?.removePrefix("Bearer "),
            request = request,
            response = response,
            securityContext = securityContext
        )
    }

    private fun ServerHttpRequest.logHeaders() {
        logger.debug {
            val headers = this.headers.map {
                if (it.key == HttpHeaders.AUTHORIZATION) {
                    it.key
                } else {
                    "${it.key}=${it.value.firstOrNull()}"
                }
            }
            "Request headers: $headers"
        }
    }
}
