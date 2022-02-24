package no.skatteetaten.aurora.gobo

import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class GoboLiveness(private val meterRegistry: MeterRegistry) {

    val nettyTotalConnections = "reactor.netty.connection.provider.total.connections"
    val nettyPendingConnections = "reactor.netty.connection.provider.pending.connections"

    fun isHealthy(): Boolean {
        meterRegistry
            .find(nettyTotalConnections)
            .gauges()
            .forEach { total ->
                val pending = meterRegistry
                    .find(nettyPendingConnections)
                    .tags(total.id.tags)
                    .gauge()?.value() ?: 0.0

                if (total.value() >= 15 && pending > 0) {
                    logger.warn("Liveness check failed, total connections ${total.value()} / pending connections $pending for tags ${total.id.tags}")
                    return false
                }
            }
        return true
    }
}
