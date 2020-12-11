package no.skatteetaten.aurora.gobo.graphql.gobo

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.loadMany
import java.time.Instant

data class GoboFieldUser(val name: String, val user: String)

data class GoboFieldUsage(val name: String, val count: Long, val clients: List<GoboClient>)

data class GoboClient(val name: String, val count: Long)

class GoboUsage {
    suspend fun usedFields(dfe: DataFetchingEnvironment, nameContains: String?): List<GoboFieldUsage> {
        val fields = dfe.loadMany<GoboUsage, GoboFieldUsage>(this)
        return if (nameContains == null) {
            fields
        } else {
            fields.filter { it.name.contains(nameContains) }
        }
    }

    suspend fun clients(dfe: DataFetchingEnvironment) =
        dfe.loadMany<GoboUsage, GoboClient>(this)
}

data class Gobo(val startTime: Instant, val usage: GoboUsage = GoboUsage())
