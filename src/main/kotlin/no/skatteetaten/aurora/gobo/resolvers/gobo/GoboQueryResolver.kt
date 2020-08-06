package no.skatteetaten.aurora.gobo.resolvers.gobo

/*
@Component
class GoboQueryResolver(private val goboInstrumentation: GoboInstrumentation) : GraphQLQueryResolver {

    private val startTime = Instant.now()

    fun gobo(): Gobo {
        val fields = goboInstrumentation.fieldUsage.fields.map { GoboField(it.key, it.value.sum()) }
        val users = goboInstrumentation.userUsage.users.map { GoboUser(it.key, it.value.sum()) }
        return Gobo(startTime, GoboUsage(fields, users))
    }
}
*/
