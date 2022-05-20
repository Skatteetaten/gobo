package no.skatteetaten.aurora.gobo.graphql

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import no.skatteetaten.aurora.gobo.ConnectionPool
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class GoboMetrics(private val meterRegistry: MeterRegistry) {

    val nettyTotalConnections = "reactor.netty.connection.provider.total.connections"
    val nettyPendingConnections = "reactor.netty.connection.provider.pending.connections"
    val nettyActiveConnections = "reactor.netty.connection.provider.active.connections"
    val nettyMaxConnections = "reactor.netty.connection.provider.max.connections"
    val nettyIdleConnections = "reactor.netty.connection.provider.idle.connections"

    val graphqlOperationTimer = "graphql.operationTimer"
    val graphqlUnfinishedQueries = "graphql.unfinishedQueries"

    fun registerQueryTimeUsed(operationName: String?, timeUsed: Long) {
        operationName?.let {
            Timer.builder(graphqlOperationTimer)
                .tags(listOf(Tag.of("operationName", it)))
                .description("Time used for graphql operation")
                .minimumExpectedValue(Duration.ofMillis(100))
                .maximumExpectedValue(Duration.ofMillis(30000))
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.ofMillis(timeUsed))
        }
    }

    fun registerUnfinshedQueries(numOfUnfinishedQueries: Int) {
        Gauge.builder(graphqlUnfinishedQueries) { numOfUnfinishedQueries }
            .description("Number of unfinished queries in gobo.")
            .register(meterRegistry)
    }

    private fun MeterRegistry.valueForGauge(name: String, gauge: Gauge) = find(name).tags(gauge.id.tags).gauge()?.value() ?: 0.0

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
}
