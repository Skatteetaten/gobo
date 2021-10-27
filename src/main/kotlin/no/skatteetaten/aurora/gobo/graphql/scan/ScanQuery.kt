package no.skatteetaten.aurora.gobo.graphql.scan

import com.expediagroup.graphql.server.operations.Query
import no.skatteetaten.aurora.gobo.graphql.scan.Scan.Companion.fromProbeResultList
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeService
import org.springframework.stereotype.Component

@Component
class ScanQuery(val scanService: ProbeService) : Query {
    suspend fun scan(host: String, port: Int? = null): Scan =
        fromProbeResultList(scanService.probeFirewall(host, port ?: 80))
}
