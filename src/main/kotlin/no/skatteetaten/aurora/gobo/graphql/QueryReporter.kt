package no.skatteetaten.aurora.gobo.graphql

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
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

const val DEFAULT_REPORT_AFTER_MILLIS: Long = 300000
const val DEFAULT_UNFINISHED_QUERIES_EXPIRE_MINUTES: Long = 60

@Component
class QueryReporter(
    @Value("\${gobo.graphql.reportAfterMillis:$DEFAULT_REPORT_AFTER_MILLIS}")
    reportAfterMillis: Long = DEFAULT_REPORT_AFTER_MILLIS,
    @Value("\${gobo.graphql.unfinishedQueriesExpireMinutes:$DEFAULT_UNFINISHED_QUERIES_EXPIRE_MINUTES}")
    unfinishedQueriesExpireMinutes: Long = DEFAULT_UNFINISHED_QUERIES_EXPIRE_MINUTES
) {
    private val unfinishedQueries = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(unfinishedQueriesExpireMinutes))
        .scheduler(Scheduler.systemScheduler())
        .build<String, QueryOperation>()

    private val queries = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMillis(reportAfterMillis))
        .scheduler(Scheduler.systemScheduler())
        .evictionListener { id: String?, query: QueryOperation?, _ ->
            if (id != null && query != null) {
                logger.warn {
                    """Unfinished query, Query-Korrelasjonsid=${query.korrelasjonsid} Query-TraceId="${query.traceid}" Query-Klientid="${query.klientid}" started="${query.started}" name=${query.name} query="${query.query}" """
                }
                unfinishedQueries.put(id, query)
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
