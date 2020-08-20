package no.skatteetaten.aurora.gobo.resolvers.scan

import com.expediagroup.graphql.spring.operations.Query
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeService
import no.skatteetaten.aurora.gobo.resolvers.scan.Scan.Companion.fromProbeResultList
import org.springframework.stereotype.Component

@Suppress("unused")
@Component
class ScanQueryResolver(val scanService: ProbeService) : Query {
    suspend fun scan(host: String, port: Int?): Scan =
        fromProbeResultList(scanService.probeFirewall(host, port ?: 80))
}
