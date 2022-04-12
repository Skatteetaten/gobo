package no.skatteetaten.aurora.gobo

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
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
        maxTotalConnections = 14,
        maxPendingConnections = 2,
        maxUnfinishedQueries = 2
    )

    @BeforeEach
    internal fun setUp() {
        meterRegistry.clear()
    }

    @Test
    fun `Get connection pools`() {
        meterRegistry.gauge(liveness.nettyTotalConnections, 15)
        meterRegistry.gauge(liveness.nettyPendingConnections, 3)
        val connectionPools = liveness.getConnectionPools()

        assertThat(connectionPools).hasSize(1)
        assertThat(connectionPools.first().totalConnections).isEqualTo(15.0)
        assertThat(connectionPools.first().pendingConnections).isEqualTo(3.0)
    }

    @Test
    fun `Get unfinished queries`() {
        queryReporter.add("test123", "klientId", "name", "query")
        val unfinished = liveness.getUnfinishedQueries()

        assertThat(unfinished).hasSize(1)
        assertThat(unfinished.first().korrelasjonsid).isEqualTo("test123")
    }
}
