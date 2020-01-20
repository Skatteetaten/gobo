package no.skatteetaten.aurora.gobo.resolvers.scan

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeServiceBlocking
import org.springframework.stereotype.Component

@Component
class ScanQueryResolver(val scanService: ProbeServiceBlocking) : GraphQLQueryResolver {

    fun scan(host: String, port: Int = 80): Scan {
        return Scan.fromProbeResultList(scanService.probeFirewall(host, port))
    }
}
