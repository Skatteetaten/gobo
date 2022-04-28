package no.skatteetaten.aurora.gobo

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.skatteetaten.aurora.gobo.graphql.QueryReporter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GoboLivenessTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val queryReporter = QueryReporter(reportAfterMillis = 0)
    private val liveness = GoboLiveness(
        meterRegistry = meterRegistry,
        queryReporter = queryReporter,
        maxUnfinishedQueries = 2
    )

    @BeforeEach
    internal fun setUp() {
        meterRegistry.clear()
        queryReporter.logAndClear()
    }

    @Test
    fun `Get connection pools`() {
        meterRegistry.gauge(liveness.nettyTotalConnections, 15)
        meterRegistry.gauge(liveness.nettyPendingConnections, 3)
        meterRegistry.gauge(liveness.nettyActiveConnections, 1)
        meterRegistry.gauge(liveness.nettyMaxConnections, 32)
        val connectionPools = liveness.getConnectionPools()

        assertThat(connectionPools).hasSize(1)
        assertThat(connectionPools.first().totalConnections).isEqualTo(15.0)
        assertThat(connectionPools.first().pendingConnections).isEqualTo(3.0)
        assertThat(connectionPools.first().activeConnections).isEqualTo(1.0)
        assertThat(connectionPools.first().maxConnections).isEqualTo(32.0)
    }

    @Test
    fun `Get unfinished queries`() {
        queryReporter.add("test123", "klientId", "name", "query")
        val unfinished = liveness.unfinishedQueries()

        assertThat(unfinished.success).isTrue()
        assertThat(unfinished.queries).hasSize(1)
        assertThat(unfinished.queries.first().korrelasjonsid).isEqualTo("test123")
    }

    @Test
    fun `Unfinished queries check fails`() {
        queryReporter.add("test123", "klientId", "name", "query")
        queryReporter.add("test456", "klientId", "name", "query")
        queryReporter.add("test789", "klientId", "name", "query")

        val unfinished = liveness.unfinishedQueries()
        assertThat(unfinished.success).isFalse()
        assertThat(unfinished.queries).hasSize(3)
    }
}
