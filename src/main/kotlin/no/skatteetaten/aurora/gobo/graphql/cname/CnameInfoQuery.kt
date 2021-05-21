package no.skatteetaten.aurora.gobo.graphql.cname

import com.expediagroup.graphql.server.operations.Query
import no.skatteetaten.aurora.gobo.integration.gavel.CnameService
import org.springframework.stereotype.Component

data class CnameEntry(val cname: String, val host: String, val ttl: Int)

data class CnameInfo(
    val status: String,
    val clusterId: String,
    val appName: String,
    val namespace: String,
    val routeName: String,
    val message: String,
    val entry: CnameEntry
)

@Component
class CnameInfoQuery(val cnameService: CnameService) : Query {

    suspend fun cnameInfo(affiliations: List<String>? = null) =
        cnameService.getCnameInfo()
}
