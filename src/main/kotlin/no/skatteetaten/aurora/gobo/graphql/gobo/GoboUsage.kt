package no.skatteetaten.aurora.gobo.graphql.gobo

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadMany
import no.skatteetaten.aurora.gobo.graphql.loadOrThrow
import java.time.Instant

data class GoboFieldUser(val name: String, val user: String)

data class GoboFieldUsage(val name: String, val count: Long, val clients: List<GoboClient>)

data class GoboClient(val name: String, val count: Long)

class GoboUsage {

    suspend fun numberOfClients(dfe: DataFetchingEnvironment) =
        dfe.loadOrThrow<GoboUsage, GoboClientCount>(this).numberOfClients

    suspend fun numberOfFields(dfe: DataFetchingEnvironment) =
        dfe.loadOrThrow<GoboUsage, GoboFieldCount>(this).numberOfFields

    suspend fun usedFields(
        dfe: DataFetchingEnvironment,
        nameContains: String?,
        mostUsedOnly: Boolean?
    ): List<GoboFieldUsage> {
        val fields = dfe.loadMany<String, GoboFieldUsage>(nameContains ?: "")
        return if (mostUsedOnly == true) {
            fields.sortedByDescending { it.count }.take(5)
        } else {
            fields
        }
    }

    suspend fun clients(dfe: DataFetchingEnvironment, nameContains: String?) =
        dfe.loadMany<String, GoboClient>(nameContains ?: "")
}

data class Gobo(val startTime: Instant, val usage: GoboUsage = GoboUsage())
