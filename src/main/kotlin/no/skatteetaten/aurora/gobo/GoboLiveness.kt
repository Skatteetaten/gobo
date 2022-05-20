package no.skatteetaten.aurora.gobo

import io.micrometer.core.instrument.Tag
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.GoboMetrics
import no.skatteetaten.aurora.gobo.graphql.QueryOperation
import no.skatteetaten.aurora.gobo.graphql.QueryReporter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping

private val logger = KotlinLogging.logger {}

data class ConnectionPool(
    val totalConnections: Double,
    val pendingConnections: Double,
    val activeConnections: Double,
    val maxConnections: Double,
    val idleConnections: Double,
    val tags: List<Tag>
)

data class UnfinishedQueries(val success: Boolean, val queries: List<QueryOperation>) {
    companion object Factory {
        fun failed(queries: List<QueryOperation>) = UnfinishedQueries(false, queries)
        fun success(queries: List<QueryOperation>) = UnfinishedQueries(true, queries)
    }
}

@Component
class GoboLiveness(
    private val queryReporter: QueryReporter,
    private val goboMetrics: GoboMetrics,
    @Value("\${gobo.liveness.maxUnfinishedQueries:4}") private val maxUnfinishedQueries: Int,
) {
    fun unfinishedQueries() =
        queryReporter
            .unfinishedQueries()
            .let {
                if (it.size > maxUnfinishedQueries) {
                    logger.warn { "Liveness check failed with ${it.size} number of unfinished queries" }
                    UnfinishedQueries.failed(it)
                } else {
                    UnfinishedQueries.success(it)
                }
            }

    /**
     * Wait 10 secs initially, then wait 10 seconds between producing each entry (can be configured)
     */
    @Scheduled(initialDelay = 10000, fixedDelayString = "\${gobo.unfinishedQueriesMetric.fixedDelay:10000}")
    fun produceUnfinishedQueriesMetric() {
        queryReporter.unfinishedQueries().let { goboMetrics.registerUnfinshedQueries(it.size) }
    }
}

@Component
@RestControllerEndpoint(id = "liveness")
class GoboLivenessController(private val goboLiveness: GoboLiveness, private val goboMetrics: GoboMetrics) {

    @GetMapping
    fun liveness(): ResponseEntity<Map<String, List<*>>> {
        logger.debug("Liveness check called")
        val connectionPools = goboMetrics.getConnectionPools()
        val unfinishedQueries = goboLiveness.unfinishedQueries()

        val response =
            mapOf("connectionPools" to connectionPools, "unfinishedQueries" to unfinishedQueries.queries)

        // Any code greater than or equal to 200 and less than 400 indicates success. Any other code indicates failure.
        return if (unfinishedQueries.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.internalServerError().body(response)
        }
    }
}
