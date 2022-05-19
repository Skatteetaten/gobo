package no.skatteetaten.aurora.gobo

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import mu.KotlinLogging
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
    private val meterRegistry: MeterRegistry,
    private val queryReporter: QueryReporter,
    @Value("\${gobo.liveness.maxUnfinishedQueries:4}") private val maxUnfinishedQueries: Int,
) {

    val nettyTotalConnections = "reactor.netty.connection.provider.total.connections"
    val nettyPendingConnections = "reactor.netty.connection.provider.pending.connections"
    val nettyActiveConnections = "reactor.netty.connection.provider.active.connections"
    val nettyMaxConnections = "reactor.netty.connection.provider.max.connections"
    val nettyIdleConnections = "reactor.netty.connection.provider.idle.connections"

    fun MeterRegistry.valueForGauge(name: String, gauge: Gauge) = find(name).tags(gauge.id.tags).gauge()?.value() ?: 0.0

    fun getConnectionPools() = meterRegistry
        .find(nettyTotalConnections)
        .gauges()
        .mapNotNull { total ->
            val pending = meterRegistry.valueForGauge(nettyPendingConnections, total)
            val active = meterRegistry.valueForGauge(nettyActiveConnections, total)
            val max = meterRegistry.valueForGauge(nettyMaxConnections, total)
            val idle = meterRegistry.valueForGauge(nettyIdleConnections, total)
            ConnectionPool(total.value(), pending, active, max, idle, total.id.tags)
        }

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
        queryReporter
            .unfinishedQueries()
            .let {
                Gauge.builder("graphql.unfinishedQueries") { it.size }
                    .description("Number of unfinished queries in gobo.")
                    .register(meterRegistry)
            }
    }
}

@Component
@RestControllerEndpoint(id = "liveness")
class GoboLivenessController(private val goboLiveness: GoboLiveness) {

    @GetMapping
    fun liveness(): ResponseEntity<Map<String, List<*>>> {
        logger.debug("Liveness check called")
        val connectionPools = goboLiveness.getConnectionPools()
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
