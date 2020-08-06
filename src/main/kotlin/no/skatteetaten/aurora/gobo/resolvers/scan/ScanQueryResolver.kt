package no.skatteetaten.aurora.gobo.resolvers.scan

import com.expediagroup.graphql.spring.operations.Query
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeServiceBlocking
import org.springframework.stereotype.Component

@Component
class ScanQueryResolver(val scanService: ProbeServiceBlocking) : Query {

    fun scan(host: String, port: Int = 80): Scan {
        return Scan.fromProbeResultList(scanService.probeFirewall(host, port))
    }
}
