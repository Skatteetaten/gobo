package no.skatteetaten.aurora.gobo.resolvers.gobo

data class GoboUsage(val usedFields: Set<String>)

data class Gobo(val usage: GoboUsage)