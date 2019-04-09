package no.skatteetaten.aurora.gobo.resolvers.gobo

import java.time.Instant

data class GoboField(val name: String, val count: Long)

data class GoboUser(val name: String, val count: Long)

data class GoboUsage(val usedFields: List<GoboField>, val users: List<GoboUser>) {
    fun usedFields(nameContains: String?) =
        if (nameContains == null) {
            usedFields
        } else {
            usedFields.filter { it.name.contains(nameContains) }
        }
}

data class Gobo(val startTime: Instant, val usage: GoboUsage)