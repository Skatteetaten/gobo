package no.skatteetaten.aurora.gobo.graphql

import brave.baggage.BaggageField
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.CoroutineInfo
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.reactive.awaitFirst
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.errorhandling.isInvalidToken
import no.skatteetaten.aurora.gobo.graphql.errorhandling.isNoSuchElementException
import no.skatteetaten.aurora.gobo.removeNewLines
import no.skatteetaten.aurora.webflux.AuroraRequestParser
import org.springframework.boot.actuate.management.ThreadDumpEndpoint
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.reactive.function.server.ServerRequest
import reactor.core.publisher.Mono
import java.time.Duration

private val logger = KotlinLogging.logger {}

val DataFetchingEnvironment.token
    get() = graphQlContext.token
val GraphQLContext.token: String
    get() = get<String>("token").ifEmpty { throw AccessDeniedException("Token is not set") }

val GraphQLContext.securityContext: Mono<SecurityContext>
    get() = get<Mono<SecurityContext>>("securityContext").cache(Duration.ofMinutes(3))

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun GraphQLContext.awaitSecurityContext(): SecurityContext {
    val coroutinesInfo = getActiveCoroutines()
    return runCatching { securityContext.awaitFirst() }
        .recoverCatching { throwable ->
            printCoroutines(coroutinesInfo)

            when {
                throwable.isNoSuchElementException() -> logger.info { "Unable to get the security context, ${throwable.message}" }
                !throwable.isInvalidToken() -> logger.info(throwable) { "Unable to get the security context" }
            }

            throw AccessDeniedException("Invalid bearer token", throwable)
        }.getOrThrow()
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun getActiveCoroutines() = if (DebugProbes.isInstalled) {
    DebugProbes.dumpCoroutinesInfo()
} else {
    null
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun printCoroutines(coroutinesInfo: List<CoroutineInfo>?) {
    if (DebugProbes.isInstalled) {
        coroutinesInfo?.let { coroutines ->
            println("Before await coroutines:\n###############")
            coroutines.forEach { coroutine -> coroutine.job?.let { DebugProbes.printJob(it) } }
        }

        println("\n\nAfter await timed out coroutines:\n###############")
        DebugProbes.dumpCoroutines()

        println("\n\nThread dump:\n###############")
        println(ThreadDumpEndpoint().textThreadDump())
    }
}

val GraphQLContext.request: ServerRequest
    get() = get("request")
val DataFetchingEnvironment.korrelasjonsid: String
    get() = graphQlContext.korrelasjonsid
val GraphQLContext.korrelasjonsid: String
    get() = request.korrelasjonsid() ?: BaggageField.getByName(AuroraRequestParser.KORRELASJONSID_FIELD)?.value ?: ""
val DataFetchingEnvironment.klientid: String?
    get() = graphQlContext.klientid
val GraphQLContext.klientid: String?
    get() = request.klientid()
val GraphQLContext.id: String
    get() = get("id")
val DataFetchingEnvironment.startTime: Long
    get() = graphQlContext.startTime

fun GraphQLContext.addStartTime() {
    put("startTime", System.currentTimeMillis())
}

val GraphQLContext.startTime: Long
    get() = get("startTime")

val GraphQLContext.coroutineScope: CoroutineScope
    get() = get("coroutineScope")

/**
 * Get the operation name defined in mutation/query.
 * If no operation name is defined, use the first resource in the query.
 */
val GraphQLContext.operationName: String
    get() = operationNameOrNull ?: query.removePrefix("{").substringBefore("{").removeNewLines()
val GraphQLContext.operationNameOrNull: String?
    get() = getOrDefault<String?>("operationName", null)

fun GraphQLContext.putOperationName(operationName: String?) {
    operationName?.let { put("operationName", it) }
}

val GraphQLContext.operationType: String?
    get() = getOrDefault<String?>("operationType", null)

fun GraphQLContext.putOperationType(operationType: String?) {
    operationType?.let { put("operationType", it.lowercase()) }
}

val DataFetchingEnvironment.query: String
    get() = graphQlContext.query
val GraphQLContext.query: String
    get() = getOrDefault("query", "")

fun GraphQLContext.putQuery(query: String) {
    put("query", query.removeNewLines())
}
