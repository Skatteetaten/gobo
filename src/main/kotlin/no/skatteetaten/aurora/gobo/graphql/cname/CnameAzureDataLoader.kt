package no.skatteetaten.aurora.gobo.graphql.cname

import graphql.GraphQLContext
import graphql.execution.DataFetcherResult
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.newDataFetcherResult
import no.skatteetaten.aurora.gobo.integration.spotless.SpotlessCnameService
import org.springframework.stereotype.Component

@Component
class CnameAzureDataLoader(
    val spotlessCnameService: SpotlessCnameService
) : GoboDataLoader<String, DataFetcherResult<List<CnameAzure>>>() {

    override suspend fun getByKeys(
        keys: Set<String>,
        ctx: GraphQLContext
    ): Map<String, DataFetcherResult<List<CnameAzure>>> {
        return keys.associateWith { affiliation ->
            runCatching {
                spotlessCnameService.getCnameContent(affiliation).let(::newDataFetcherResult)
            }.recoverCatching { e -> newDataFetcherResult(emptyList(), listOf(e)) }
                .getOrThrow()
        }
    }
}
