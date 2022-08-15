package no.skatteetaten.aurora.gobo.graphql.cname

import com.expediagroup.graphql.server.operations.Query
import no.skatteetaten.aurora.gobo.integration.gavel.CnameService
import org.springframework.stereotype.Component

@Component
class CnameInfoQuery(val cnameService: CnameService) : Query {

    suspend fun cnameInfo(affiliations: List<String>? = null): List<CnameInfo> {
        val cnameInfo = cnameService.getCnameInfo()
        return affiliations?.let { cnameInfo.filter { it.containsAffiliation(affiliations) } } ?: cnameInfo
    }
}
