package no.skatteetaten.aurora.gobo.graphql.storagegrid

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadValue

data class Storagegrid(
    @GraphQLIgnore
    val name: String
) {
    fun tenant(dfe: DataFetchingEnvironment) = dfe.loadValue<String, StoragegridTenant>(name)

    fun objectAreas(dfe: DataFetchingEnvironment) = ObjectAreas(name)
}

data class ObjectAreas(
    @GraphQLIgnore
    val name: String
) {
    fun active(dfe: DataFetchingEnvironment) = dfe.loadValue<String, List<StoragegridObjectArea>>(name)
}
