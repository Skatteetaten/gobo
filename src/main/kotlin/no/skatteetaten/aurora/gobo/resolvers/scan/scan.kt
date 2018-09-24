package no.skatteetaten.aurora.gobo.resolvers.scan

import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeResult
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeStatus

data class Scan(
    val status: ScanStatus,
    val hostName: String?,
    val resolvedIp: String?,
    val port: Int?,
    val open: NodeDetailsConnection,
    val failed: NodeDetailsConnection
) {
    companion object {
        fun fromProbeResultList(probeResultList: List<ProbeResult>): Scan {
            if (probeResultList.isEmpty()) {
                throw SourceSystemException("Received empty result")
            }

            val firstResult = probeResultList.first().result

            return Scan(
                getAggregatedStatus(probeResultList),
                firstResult?.dnsname,
                firstResult?.resolvedIp,
                firstResult?.port?.toInt(),
                createOpen(probeResultList),
                createFailed(probeResultList)
            )
        }

        private fun getAggregatedStatus(probeResultList: List<ProbeResult>): ScanStatus {
            return if (probeResultList.all { it.result?.status?.equals(ProbeStatus.OPEN) ?: false }) {
                ScanStatus.OPEN
            } else {
                ScanStatus.CLOSED
            }
        }

        private fun mapStatus(probeStatus: ProbeStatus): ScanStatus {
            return try {
                ScanStatus.valueOf(probeStatus.name)
            } catch (e: IllegalArgumentException) {
                ScanStatus.UNKNOWN
            }
        }

        private fun createFailed(probeResultList: List<ProbeResult>): NodeDetailsConnection {
            return createNodeDetails(probeResultList) { status -> status != ProbeStatus.OPEN }
        }

        private fun createOpen(probeResultList: List<ProbeResult>): NodeDetailsConnection {
            return createNodeDetails(probeResultList) { status -> status == ProbeStatus.OPEN }
        }

        private fun createNodeDetails(probeResultList: List<ProbeResult>, condition: (ProbeStatus) -> Boolean): NodeDetailsConnection {
            return NodeDetailsConnection(probeResultList.filter { condition(it.result?.status ?: ProbeStatus.UNKNOWN) }.map {
                NodeDetailsEdge(NodeDetails(mapStatus(it.result?.status ?: ProbeStatus.UNKNOWN), it.result?.message, ClusterNode(it.hostIp))) })
        }
    }
}

data class NodeDetails(
    val status: ScanStatus?,
    val message: String?,
    val clusterNode: ClusterNode
)

data class ClusterNode(
    val ip: String?
)

data class NodeDetailsEdge(private val node: NodeDetails)

data class NodeDetailsConnection(
    val edges: List<NodeDetailsEdge>,
    val totalCount: Int = edges.size
)

enum class ScanStatus {
    ERROR,
    DNS_FAILED,
    DNS_SUCCESS,
    OPEN,
    CLOSED,
    FILTERED,
    UNKNOWN
}
