package no.skatteetaten.aurora.gobo.graphql.cname

import graphql.GraphQLContext
import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.newDataFetcherResult
import no.skatteetaten.aurora.gobo.integration.gavel.CnameService
import org.springframework.stereotype.Component

@Component
class CnameInfoDataLoader(
    val cnameService: CnameService
) : GoboDataLoader<String, DataFetcherResult<List<CnameInfo>>>() {

    override suspend fun getByKeys(
        keys: Set<String>,
        ctx: GraphQLContext
    ): Map<String, DataFetcherResult<List<CnameInfo>>> {
        return keys.associateWith { affiliation ->
            runCatching {
                cnameService.getCnameInfo(affiliation).let(::newDataFetcherResult)
            }.recoverCatching { e -> newDataFetcherResult(emptyList(), listOf(e)) }
                .getOrThrow()
        }
    }
}
