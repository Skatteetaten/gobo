package no.skatteetaten.aurora.gobo.graphql.gobo

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadListValue
import no.skatteetaten.aurora.gobo.graphql.loadMany
import no.skatteetaten.aurora.gobo.graphql.loadValue
import java.time.Instant

data class GoboFieldUser(val name: String, val user: String)

data class GoboFieldUsage(val name: String, val count: Long, val clients: List<GoboClient>)

data class GoboClient(val name: String, val count: Long)

class GoboUsage {

    fun numberOfClients(dfe: DataFetchingEnvironment) =
        dfe.loadValue<GoboUsage, Long>(key = this, loaderClass = GoboClientCountBatchDataLoader::class)

    fun numberOfFields(dfe: DataFetchingEnvironment) =
        dfe.loadValue<GoboUsage, Long>(key = this, loaderClass = GoboFieldCountBatchDataLoader::class)

    suspend fun usedFields(
        dfe: DataFetchingEnvironment,
        nameContains: String? = null,
        mostUsedOnly: Boolean? = null
    ): List<GoboFieldUsage> {
        val fields = dfe.loadMany<String, GoboFieldUsage>(nameContains ?: "")
        return if (mostUsedOnly == true) {
            fields.sortedByDescending { it.count }.take(5)
        } else {
            fields
        }
    }

    fun clients(dfe: DataFetchingEnvironment, nameContains: String? = null) =
        dfe.loadListValue<String, GoboClient>(nameContains ?: "")
}

data class Gobo(val startTime: Instant, val usage: GoboUsage = GoboUsage())
