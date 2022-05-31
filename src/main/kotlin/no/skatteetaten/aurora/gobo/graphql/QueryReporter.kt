package no.skatteetaten.aurora.gobo.graphql

import brave.Tracer
import com.github.benmanes.caffeine.cache.Caffeine
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

data class QueryOperation(
    val korrelasjonsid: String,
    val traceid: String?,
    val name: String,
    val klientid: String?,
    val query: String,
    val started: LocalDateTime = LocalDateTime.now()
)

private val logger = KotlinLogging.logger {}

@Component
class QueryReporter(
    reportAfterMillis: Long = 300000,
    private val tracer: Tracer? = null
) {
    private val unfinishedQueries = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build<String, QueryOperation>()

    private val queries = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMillis(reportAfterMillis))
        .evictionListener { korrelasjonsid: String?, query: QueryOperation?, _ ->
            if (korrelasjonsid != null && query != null) {
                logger.warn {
                    """Unfinished query, Korrelasjonsid=${query.korrelasjonsid} Query-TraceId="${query.traceid}" Klientid="${query.klientid}" started="${query.started}" name=${query.name} query="${query.query}" """
                }
                unfinishedQueries.put(korrelasjonsid, query)
            }
        }.build<String, QueryOperation>()

    fun add(korrelasjonsid: String, klientid: String?, name: String, query: String) {
        if (korrelasjonsid.isNotEmpty()) {
            queries.put(
                korrelasjonsid,
                QueryOperation(
                    korrelasjonsid = korrelasjonsid,
                    traceid = tracer?.currentSpan()?.context()?.traceIdString(),
                    name = name,
                    klientid = klientid,
                    query = query
                )
            )
        }
    }

    fun remove(korrelasjonsid: String) {
        unfinishedQueries.invalidate(korrelasjonsid)
    }

    fun clear() {
        queries.invalidateAll()
        unfinishedQueries.invalidateAll()
    }

    fun unfinishedQueries(): List<QueryOperation> = unfinishedQueries.asMap().values.toList()
}
