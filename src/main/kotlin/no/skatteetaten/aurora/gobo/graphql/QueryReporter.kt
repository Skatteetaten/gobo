package no.skatteetaten.aurora.gobo.graphql

import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

data class QueryOperation(
    val korrelasjonsid: String,
    val name: String,
    val klientid: String?,
    val query: String,
    val started: LocalDateTime
)

@Component
class QueryReporter(val reportAfterMillis: Long = 300000) {
    private val queries = ConcurrentHashMap<String, QueryOperation>()

    fun add(korrelasjonsid: String, klientid: String?, name: String, query: String) {
        if (korrelasjonsid.isNotEmpty()) {
            queries[korrelasjonsid] = QueryOperation(korrelasjonsid, name, klientid, query, LocalDateTime.now())
        }
    }

    fun remove(korrelasjonsid: String) {
        queries.remove(korrelasjonsid)
    }

    fun clear() = queries.clear()

    fun unfinishedQueries() =
        queries.values.toList()
            .filter {
                val configuredPointInTime = LocalDateTime.now().minus(Duration.ofMillis(reportAfterMillis))
                it.started.isBefore(configuredPointInTime)
            }
}
