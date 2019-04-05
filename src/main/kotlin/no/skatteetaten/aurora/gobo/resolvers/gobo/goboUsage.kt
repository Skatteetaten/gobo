package no.skatteetaten.aurora.gobo.resolvers.gobo

data class GoboField(val name: String, val count: Long)

data class GoboUsage(val usedFields: List<GoboField>)

data class Gobo(val usage: GoboUsage)