package no.skatteetaten.aurora.gobo.graphql.gobo

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadValue
import java.time.Instant
import java.util.concurrent.CompletableFuture

data class GoboFieldUser(val name: String, val user: String)

data class GoboFieldUsage(val name: String, val count: Long, val clients: List<GoboClient>)

data class GoboClient(val name: String, val count: Long)

class GoboUsage {

    fun numberOfClients(dfe: DataFetchingEnvironment) =
        dfe.loadValue<GoboUsage, Long>(key = this, loaderClass = GoboClientCountBatchDataLoader::class)

    fun numberOfFields(dfe: DataFetchingEnvironment) =
        dfe.loadValue<GoboUsage, Long>(key = this, loaderClass = GoboFieldCountBatchDataLoader::class)

    fun usedFields(
        dfe: DataFetchingEnvironment,
        nameContains: String? = null,
        mostUsedOnly: Boolean? = null
    ) = dfe.loadValue<GoboFieldUsageKey, List<GoboFieldUsage>>(GoboFieldUsageKey(nameContains, mostUsedOnly == true))

    fun clients(dfe: DataFetchingEnvironment, nameContains: String? = null): CompletableFuture<List<GoboClient>> =
        dfe.loadValue(nameContains ?: "")
}

data class Gobo(val startTime: Instant, val usage: GoboUsage = GoboUsage())
