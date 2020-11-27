package no.skatteetaten.aurora.gobo.graphql.gobo

import java.time.Instant

data class GoboFieldCounter(val name: String, val count: Long)

data class GoboFieldUsage(val name: String, val count: Long, val clients: List<GoboUser>)

data class GoboUser(val name: String, val count: Long)

data class GoboUsage(val usedFields: List<GoboFieldCounter>, val users: List<GoboUser>) {
    fun usedFields(nameContains: String?) =
        if (nameContains == null) {
            usedFields
        } else {
            usedFields.filter { it.name.contains(nameContains) }
        }
}

data class Gobo(val startTime: Instant, val usage: GoboUsage)
