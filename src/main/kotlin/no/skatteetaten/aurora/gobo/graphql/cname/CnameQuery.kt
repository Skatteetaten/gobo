package no.skatteetaten.aurora.gobo.graphql.cname

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.server.operations.Query
import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.newDataFetcherResult
import no.skatteetaten.aurora.gobo.integration.gavel.CnameService
import no.skatteetaten.aurora.gobo.integration.spotless.SpotlessCnameService
import org.springframework.stereotype.Component

@Component
class CnameQuery(
    val cnameService: CnameService,
    val spotlessCnameService: SpotlessCnameService
) : Query {

    suspend fun cname(affiliations: List<String>? = null): Cname {
        return Cname(affiliations, cnameService, spotlessCnameService)
    }
}

data class Cname(
    @GraphQLIgnore
    val affiliations: List<String>?,
    @GraphQLIgnore
    val cnameService: CnameService,
    @GraphQLIgnore
    val spotlessCnameService: SpotlessCnameService
) {
    suspend fun azure(): DataFetcherResult<List<CnameAzure>> {
        return try {
            val cnameList = spotlessCnameService.getCnameContent()
            val filtered = affiliations?.let { cnameList.filter { it.containsAffiliation(affiliations) } } ?: cnameList
            newDataFetcherResult(filtered, emptyList())
        } catch (e: Exception) {
            newDataFetcherResult(emptyList(), listOf(e))
        }
    }

    suspend fun onPrem(): DataFetcherResult<List<CnameInfo>> {
        return try {
            val cnameList = cnameService.getCnameInfo()
            val filtered = affiliations?.let { cnameList.filter { it.containsAffiliation(affiliations) } } ?: cnameList
            newDataFetcherResult(filtered, emptyList())
        } catch (e: Exception) {
            newDataFetcherResult(emptyList(), listOf(e))
        }
    }
}
