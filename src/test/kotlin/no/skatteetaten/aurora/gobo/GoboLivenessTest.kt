package no.skatteetaten.aurora.gobo

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GoboLivenessTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val liveness = GoboLiveness(meterRegistry)

    @BeforeEach
    internal fun setUp() {
        meterRegistry.clear()
    }

    @Test
    fun `Liveness is healthy`() {
        meterRegistry.gauge(liveness.nettyTotalConnections, 1)
        meterRegistry.gauge(liveness.nettyPendingConnections, 0)

        assertThat(liveness.isHealthy()).isTrue()
    }

    @Test
    fun `Liveness is unhealthy with too many connections`() {
        meterRegistry.gauge(liveness.nettyTotalConnections, 15)
        meterRegistry.gauge(liveness.nettyPendingConnections, 1)

        assertThat(liveness.isHealthy()).isFalse()
    }
}
