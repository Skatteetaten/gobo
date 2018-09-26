package no.skatteetaten.aurora.gobo.resolvers.scan

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeService
import org.springframework.stereotype.Component

@Component
class ScanQueryResolver(val scanService: ProbeService) : GraphQLQueryResolver {

    fun scan(host: String, port: Int = 80): Scan {
        return Scan.fromProbeResultList(scanService.probeFirewall(host, port))
    }
}