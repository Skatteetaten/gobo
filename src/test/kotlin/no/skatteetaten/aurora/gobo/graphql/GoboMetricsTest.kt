package no.skatteetaten.aurora.gobo.graphql

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GoboMetricsTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val metrics = GoboMetrics(meterRegistry)

    @BeforeEach
    internal fun setUp() {
        meterRegistry.clear()
    }

    @Test
    fun `Get connection pools`() {
        meterRegistry.gauge(metrics.nettyTotalConnections, 15)
        meterRegistry.gauge(metrics.nettyPendingConnections, 3)
        meterRegistry.gauge(metrics.nettyActiveConnections, 1)
        meterRegistry.gauge(metrics.nettyMaxConnections, 32)
        meterRegistry.gauge(metrics.nettyIdleConnections, 4)
        val connectionPools = metrics.getConnectionPools()

        assertThat(connectionPools).hasSize(1)
        assertThat(connectionPools.first().totalConnections).isEqualTo(15.0)
        assertThat(connectionPools.first().pendingConnections).isEqualTo(3.0)
        assertThat(connectionPools.first().activeConnections).isEqualTo(1.0)
        assertThat(connectionPools.first().maxConnections).isEqualTo(32.0)
        assertThat(connectionPools.first().idleConnections).isEqualTo(4.0)
    }

    @Test
    fun `Register query time used`() {
        metrics.registerQueryTimeUsed("getAffiliations", 1000)

        assertThat(meterRegistry.meters).hasSize(1)
        assertThat(meterRegistry.meters.first().id.name).isEqualTo(metrics.graphqlOperationTimer)
    }

    @Test
    fun `Register unfinished queries`() {
        metrics.registerUnfinshedQueries(5)

        assertThat(meterRegistry.meters).hasSize(1)
        assertThat(meterRegistry.meters.first().id.name).isEqualTo(metrics.graphqlUnfinishedQueries)
    }
}
