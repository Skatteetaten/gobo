package no.skatteetaten.aurora.gobo.graphql

import brave.baggage.BaggageField
import com.expediagroup.graphql.server.spring.execution.SpringGraphQLContext
import com.expediagroup.graphql.server.spring.execution.SpringGraphQLContextFactory
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.ReactorContext
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.errorhandling.isInvalidToken
import no.skatteetaten.aurora.gobo.graphql.errorhandling.isNoSuchElementException
import no.skatteetaten.aurora.webflux.AuroraRequestParser
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono
import kotlin.coroutines.coroutineContext

fun DataFetchingEnvironment.token() = this.getContext<GoboGraphQLContext>().token()

private val logger = KotlinLogging.logger {}

class GoboGraphQLContext(
    private val token: String?,
    val request: ServerRequest,
    val securityContext: Mono<SecurityContext>,
    var query: String? = null
) : SpringGraphQLContext(request = request) {
    suspend fun securityContext(): SecurityContext = runCatching { securityContext.awaitFirst() }
        .recoverCatching {
            when {
                it.isNoSuchElementException() -> logger.info { "Unable to get the security context, ${it.message}" }
                !it.isInvalidToken() -> logger.info(it) { "Unable to get the security context" }
            }

            throw AccessDeniedException("Invalid bearer token", it)
        }
        .getOrThrow()

    fun token() = token ?: throw AccessDeniedException("Token is not set")
    fun korrelasjonsid() =
        request.korrelasjonsid() ?: BaggageField.getByName(AuroraRequestParser.KORRELASJONSID_FIELD)?.value ?: ""

    fun klientid() = request.klientid()
}

@Component
class GoboGraphQLContextFactory : SpringGraphQLContextFactory<GoboGraphQLContext>() {

    @ExperimentalCoroutinesApi
    override suspend fun generateContext(request: ServerRequest): GoboGraphQLContext? {
        request.logHeaders()

        val reactorContext =
            coroutineContext[ReactorContext]?.context ?: throw RuntimeException("Reactor Context unavailable")
        val securityContext = reactorContext.getOrDefault<Mono<SecurityContext>>(
            SecurityContext::class.java,
            Mono.error(AccessDeniedException("Security Context unavailable"))
        )!!

        return GoboGraphQLContext(
            token = request.headers().firstHeader(HttpHeaders.AUTHORIZATION)?.removePrefix("Bearer "),
            request = request,
            securityContext = securityContext
        )
    }

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
