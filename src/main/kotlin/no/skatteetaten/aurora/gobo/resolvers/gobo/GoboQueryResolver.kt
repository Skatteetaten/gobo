package no.skatteetaten.aurora.gobo.resolvers.gobo

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.resolvers.GoboInstrumentation
import org.springframework.stereotype.Component

@Component
class GoboQueryResolver(private val goboInstrumentation: GoboInstrumentation) : GraphQLQueryResolver {

    fun gobo(): Gobo {
        val counters = goboInstrumentation.usage.fields.map { GoboField(it.key, it.value.sum()) }
        return Gobo(GoboUsage(counters))
    }
}