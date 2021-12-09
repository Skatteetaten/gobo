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
private class ContextMap(val toMap: MutableMap<String, Any> = mutableMapOf()) {
    var token: String by toMap
    var securityContext: Mono<SecurityContext> by toMap
    var request: ServerRequest by toMap
    var startTime: LocalDateTime by toMap
}

private val logger = KotlinLogging.logger {}

@Component
class GoboGraphQLContextFactory : SpringGraphQLContextFactory<SpringGraphQLContext>() {

    override suspend fun generateContextMap(serverRequest: ServerRequest): Map<*, Any> {
        serverRequest.logHeaders()

        return ContextMap().apply {
            token = (serverRequest.headers().firstHeader(HttpHeaders.AUTHORIZATION)?.removePrefix("Bearer ") ?: "")
            securityContext = getSecurityContext()
            request = serverRequest
            startTime = LocalDateTime.now()
        }.toMap
    }

    private suspend fun getSecurityContext(): Mono<SecurityContext> {
        val reactorContext =
            coroutineContext[ReactorContext]?.context ?: throw RuntimeException("Reactor Context unavailable")
        return reactorContext.getOrDefault<Mono<SecurityContext>>(
            SecurityContext::class.java,
            Mono.error(AccessDeniedException("Security Context unavailable"))
        )!!
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
