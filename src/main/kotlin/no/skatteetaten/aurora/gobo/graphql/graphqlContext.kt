package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.execution.GraphQLContext
import com.expediagroup.graphql.spring.execution.GraphQLContextFactory
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.lang.IllegalArgumentException
import kotlin.coroutines.coroutineContext

fun DataFetchingEnvironment.token() = this.getContext<GoboGraphQLContext>().token
    ?: throw IllegalArgumentException("Token is not set")

class GoboGraphQLContext(val token: String?, val request: ServerHttpRequest, val response: ServerHttpResponse, val securityContext: SecurityContext?) :
    GraphQLContext

@Component
class GoboGraphQLContextFactory : GraphQLContextFactory<GoboGraphQLContext> {

    override suspend fun generateContext(request: ServerHttpRequest, response: ServerHttpResponse): GoboGraphQLContext {
        val reactorContext =
            coroutineContext[ReactorContext]?.context ?: throw RuntimeException("Reactor Context unavailable")
        val securityContext = reactorContext.getOrDefault<Mono<SecurityContext>>(
            SecurityContext::class.java,
            Mono.error(AccessDeniedException("Security Context unavailable"))
        )!!

        return GoboGraphQLContext(
            token = request.headers.getFirst("Authorization")?.removePrefix("Bearer "),
            request = request,
            response = response,
            securityContext = securityContext.awaitFirstOrNull()
        )
    }
}
