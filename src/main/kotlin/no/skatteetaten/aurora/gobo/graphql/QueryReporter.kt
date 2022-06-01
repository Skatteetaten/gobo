package no.skatteetaten.aurora.gobo.graphql

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
    reportAfterMillis: Long = 300000
) {
    private val unfinishedQueries = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build<String, QueryOperation>()

    private val queries = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMillis(reportAfterMillis))
        .evictionListener { korrelasjonsid: String?, query: QueryOperation?, _ ->
            if (korrelasjonsid != null && query != null) {
                logger.warn {
                    """Unfinished query, Query-Korrelasjonsid=${query.korrelasjonsid} Query-TraceId="${query.traceid}" Query-Klientid="${query.klientid}" started="${query.started}" name=${query.name} query="${query.query}" """
                }
                unfinishedQueries.put(korrelasjonsid, query)
            }
        }.build<String, QueryOperation>()

    fun add(id: String, traceId: String?, korrelasjonsid: String, klientid: String?, name: String, query: String) {
        queries.put(
            id,
            QueryOperation(
                korrelasjonsid = korrelasjonsid,
                traceid = traceId,
                name = name,
                klientid = klientid,
                query = query
            )
        )
    }

    fun remove(id: String) {
        queries.invalidate(id)
    }

    fun clear() {
        queries.invalidateAll()
        unfinishedQueries.invalidateAll()
    }

    fun queries() = queries.asMap().values.toList()

    fun unfinishedQueries() = unfinishedQueries.asMap().values.toList()
}
