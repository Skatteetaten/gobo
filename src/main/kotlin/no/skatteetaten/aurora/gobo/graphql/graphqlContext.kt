package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.execution.GraphQLContext
import com.expediagroup.graphql.spring.execution.GraphQLContextFactory
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.ReactorContext
import mu.KotlinLogging
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
    val securityContext: SecurityContext?
) : GraphQLContext {
    fun token() = token ?: throw AccessDeniedException("Token is not set")
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
            securityContext = securityContext.awaitFirstOrNull()
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
