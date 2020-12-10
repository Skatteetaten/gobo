package no.skatteetaten.aurora.gobo.graphql.gobo

import java.time.Instant

data class GoboFieldUser(val name: String, val user: String)

data class GoboFieldUsage(val name: String, val count: Long, val clients: List<GoboClient>)

data class GoboClient(val name: String, val count: Long)

data class GoboUsage(val usedFields: List<GoboFieldUsage>, val clients: List<GoboClient>) {
    fun usedFields(nameContains: String?) =
        if (nameContains == null) {
            usedFields
        } else {
            usedFields.filter { it.name.contains(nameContains) }
        }
}

data class Gobo(val startTime: Instant, val usage: GoboUsage)
