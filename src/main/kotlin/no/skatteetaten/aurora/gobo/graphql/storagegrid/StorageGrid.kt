package no.skatteetaten.aurora.gobo.graphql.storagegrid

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadValue

data class StorageGrid(
    @GraphQLIgnore
    val name: String
) {
    fun tenant(dfe: DataFetchingEnvironment) = dfe.loadValue<String, StorageGridTenant>(name)

    fun objectAreas(dfe: DataFetchingEnvironment) = StorageGridObjectAreas(name)
}

data class StorageGridObjectAreas(
    @GraphQLIgnore
    val name: String
) {
    fun active(dfe: DataFetchingEnvironment) = dfe.loadValue<String, List<StorageGridObjectArea>>(name)
}
