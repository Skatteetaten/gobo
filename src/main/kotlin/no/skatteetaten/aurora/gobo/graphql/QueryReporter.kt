package no.skatteetaten.aurora.gobo.graphql

import brave.Tracer
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

data class QueryOperation(
    val korrelasjonsid: String,
    val traceid: String?,
    val name: String,
    val klientid: String?,
    val query: String,
    val started: LocalDateTime
)

private val logger = KotlinLogging.logger {}

@Component
class QueryReporter(
    private val reportAfterMillis: Long = 300000,
    private val tracer: Tracer? = null
) {
    private val queries = ConcurrentHashMap<String, QueryOperation>()

    fun add(korrelasjonsid: String, klientid: String?, name: String, query: String) {
        if (korrelasjonsid.isNotEmpty()) {
            queries[korrelasjonsid] = QueryOperation(
                korrelasjonsid = korrelasjonsid,
                traceid = tracer?.currentSpan()?.context()?.traceIdString(),
                name = name,
                klientid = klientid,
                query = query,
                started = LocalDateTime.now()
            )
        }
    }

    fun remove(korrelasjonsid: String) {
        queries.remove(korrelasjonsid)
    }

    fun logAndClear() {
        queries.values.forEach {
            logger.warn { """Unfinished query, Korrelasjonsid=${it.korrelasjonsid} Query-TraceId="${it.traceid}" Klientid="${it.klientid}" started="${it.started}" name=${it.name} query="${it.query}" """ }
        }

        queries.clear()
    }

    fun unfinishedQueries() =
        queries.values.toList()
            .filter {
                val configuredPointInTime = LocalDateTime.now().minus(Duration.ofMillis(reportAfterMillis))
                it.started.isBefore(configuredPointInTime)
            }
}
