package no.skatteetaten.aurora.gobo.graphql

import org.apache.commons.collections4.queue.CircularFifoQueue
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDateTime

data class QueryEntry(val time: LocalDateTime, val klientid: String?, val korrelasjonsid: String?, val query: String)

@Component
@ConditionalOnProperty(value = ["gobo.graphql.cachequeries"], havingValue = "true", matchIfMissing = true)
class QueryCache : InfoContributor {

    private val cache = CircularFifoQueue<QueryEntry>(100)

    fun add(klientid: String?, korrelasjonsid: String?, query: String) {
        cache.add(QueryEntry(LocalDateTime.now(), klientid, korrelasjonsid, query))
    }

    fun get() = cache.toList()

    fun clear() = cache.clear()

    override fun contribute(builder: Info.Builder?) {
        builder?.withDetails(mapOf("queries" to cache.toList()))
    }
}
