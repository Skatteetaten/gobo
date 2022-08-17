package no.skatteetaten.aurora.gobo.graphql.cname

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadValue

data class Cname(@GraphQLIgnore val affiliation: String) {

    fun azure(dfe: DataFetchingEnvironment) = dfe.loadValue<String, DataFetcherResult<List<CnameAzure>>>(affiliation, loaderClass = CnameAzureDataLoader::class)

    fun onPrem(dfe: DataFetchingEnvironment) = dfe.loadValue<String, DataFetcherResult<List<CnameInfo>>>(affiliation, loaderClass = CnameInfoDataLoader::class)
}
