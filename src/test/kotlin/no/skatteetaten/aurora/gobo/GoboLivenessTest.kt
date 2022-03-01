package no.skatteetaten.aurora.gobo

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GoboLivenessTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val liveness = GoboLiveness(
        meterRegistry = meterRegistry,
        maxTotalConnections = 14,
        maxPendingConnections = 2
    )

    @BeforeEach
    internal fun setUp() {
        meterRegistry.clear()
    }

    @Test
    fun `Does not have connection pool problems`() {
        meterRegistry.gauge(liveness.nettyTotalConnections, 1)
        meterRegistry.gauge(liveness.nettyPendingConnections, 0)

        assertThat(liveness.getConnectionPoolProblems()).isEmpty()
    }

    @Test
    fun `Connection pool problems with too many connections`() {
        meterRegistry.gauge(liveness.nettyTotalConnections, 15)
        meterRegistry.gauge(liveness.nettyPendingConnections, 3)
        val connectionPoolProblems = liveness.getConnectionPoolProblems()

        assertThat(connectionPoolProblems).hasSize(1)
        assertThat(connectionPoolProblems.first().totalConnections).isEqualTo(15.0)
        assertThat(connectionPoolProblems.first().pendingConnections).isEqualTo(3.0)
    }
}
