package no.skatteetaten.aurora.gobo.graphql

import com.expediagroup.graphql.server.spring.execution.SpringGraphQLContext
import com.expediagroup.graphql.server.spring.execution.SpringGraphQLContextFactory
import graphql.GraphQLContext
import kotlinx.coroutines.reactor.ReactorContext
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import kotlin.coroutines.coroutineContext

class GoboGraphQLContext(val context: GraphQLContext, request: ServerRequest) : SpringGraphQLContext(request)

private val logger = KotlinLogging.logger {}

@Component
class GoboGraphQLContextFactory : SpringGraphQLContextFactory<SpringGraphQLContext>() {

    override suspend fun generateContextMap(request: ServerRequest): Map<*, Any>? {
        request.logHeaders()

        val reactorContext =
            coroutineContext[ReactorContext]?.context ?: throw RuntimeException("Reactor Context unavailable")
        val securityContext = reactorContext.getOrDefault<Mono<SecurityContext>>(
            SecurityContext::class.java,
            Mono.error(AccessDeniedException("Security Context unavailable"))
        )!!

        return mapOf(
            "token" to (request.headers().firstHeader(HttpHeaders.AUTHORIZATION)?.removePrefix("Bearer ") ?: ""),
            "securityContext" to securityContext,
            "request" to request,
            "startTime" to LocalDateTime.now()
        )
    }

    override suspend fun generateContext(request: ServerRequest) = GoboGraphQLContext(
        GraphQLContext.newContext().of(generateContextMap(request)).build(),
        request
    )

    private fun ServerRequest.logHeaders() {
        logger.debug {
            val headers = headers().asHttpHeaders().map {
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
