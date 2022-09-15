package no.skatteetaten.aurora.gobo

import com.sun.management.UnixOperatingSystemMXBean
import io.micrometer.core.instrument.Tag
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.graphql.GoboMetrics
import no.skatteetaten.aurora.gobo.graphql.QueryOperation
import no.skatteetaten.aurora.gobo.graphql.QueryReporter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import java.lang.management.ManagementFactory

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

data class NumberOfOpenFileDescriptors(val success: Boolean, val openFileDescriptors: Long) {
    companion object Factory {
        fun failed(openFileDescriptors: Long) =
            NumberOfOpenFileDescriptors(false, openFileDescriptors)

        fun success(openFileDescriptors: Long) =
            NumberOfOpenFileDescriptors(true, openFileDescriptors)
    }
}

@Component
class GoboLiveness(
    private val queryReporter: QueryReporter,
    @Value("\${gobo.liveness.maxUnfinishedQueries:4}") private val maxUnfinishedQueries: Int,
    @Value("\${gobo.liveness.maxOpenFileDescriptors:350}") private val maxOpenFileDescriptors: Long,
) {

    fun unfinishedQueries() =
        queryReporter
            .unfinishedQueries()
            .let {
                if (it.size > maxUnfinishedQueries) {
                    logger.warn { "Liveness check failed with ${it.size} number of unfinished queries, maxUnfinishedQueries=$maxUnfinishedQueries" }
                    UnfinishedQueries.failed(it)
                } else {
                    UnfinishedQueries.success(it)
                }
            }

    fun openFileDescriptors() =
        numberOfOpenFileDescriptors().let {
            if (it > maxOpenFileDescriptors) {
                logger.warn { "Liveness check failed with $it number of open file descriptors, maxOpenFileDescriptors=$maxOpenFileDescriptors" }
                NumberOfOpenFileDescriptors.failed(it)
            } else {
                NumberOfOpenFileDescriptors.success(it)
            }
        }

    private fun numberOfOpenFileDescriptors(): Long = ManagementFactory.getOperatingSystemMXBean().let {
        return when (it) {
            is UnixOperatingSystemMXBean -> it.openFileDescriptorCount
            else -> 0
        }
    }
}

@Component
@RestControllerEndpoint(id = "liveness")
class GoboLivenessController(private val goboLiveness: GoboLiveness, private val goboMetrics: GoboMetrics) {

    @GetMapping
    fun liveness(): ResponseEntity<Map<String, Any>> {
        logger.debug("Liveness check called")
        val connectionPools = goboMetrics.getConnectionPools()
        val unfinishedQueries = goboLiveness.unfinishedQueries()
        val numberOfOpenFileDescriptors = goboLiveness.openFileDescriptors()

        val response =
            mapOf(
                "connectionPools" to connectionPools,
                "unfinishedQueries" to unfinishedQueries.queries,
                "openFileDescriptors" to numberOfOpenFileDescriptors.openFileDescriptors
            )

        // Any code greater than or equal to 200 and less than 400 indicates success. Any other code indicates failure.
        return if (unfinishedQueries.success && numberOfOpenFileDescriptors.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.internalServerError().body(response)
        }
    }
}
