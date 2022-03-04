package no.skatteetaten.aurora.gobo

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping

private val logger = KotlinLogging.logger {}

data class ConnectionPool(val totalConnections: Double, val pendingConnections: Double, val tags: List<Tag>)

@Component
class GoboLiveness(
    private val meterRegistry: MeterRegistry,
    @Value("\${gobo.liveness.maxTotalConnections:14}") private val maxTotalConnections: Int,
    @Value("\${gobo.liveness.maxPendingConnections:2}") private val maxPendingConnections: Int
) {

    val nettyTotalConnections = "reactor.netty.connection.provider.total.connections"
    val nettyPendingConnections = "reactor.netty.connection.provider.pending.connections"

    fun getConnectionPools() = meterRegistry
        .find(nettyTotalConnections)
        .gauges()
        .mapNotNull { total ->
            val pending = meterRegistry
                .find(nettyPendingConnections)
                .tags(total.id.tags)
                .gauge()?.value() ?: 0.0

            ConnectionPool(total.value(), pending, total.id.tags).also {
                if (total.value() > maxTotalConnections && pending > maxPendingConnections) {
                    logger.warn("Liveness check failed, total connections ${it.totalConnections} / pending connections ${it.pendingConnections} for tags ${it.tags}")
                }
            }
        }
}

@Component
@RestControllerEndpoint(id = "liveness")
class GoboLivenessController(private val goboLiveness: GoboLiveness) {

    @GetMapping
    fun liveness(): ResponseEntity<Map<String, List<ConnectionPool>>> {
        logger.debug("Liveness check called")
        val connectionPools = goboLiveness.getConnectionPools()
        return ResponseEntity.ok(mapOf("connectionPools" to connectionPools))
    }
}
