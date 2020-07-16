package no.skatteetaten.aurora.gobo.resolvers.gobo

import com.expediagroup.graphql.spring.operations.Query
import java.time.Instant
import no.skatteetaten.aurora.gobo.resolvers.GoboInstrumentation
import org.springframework.stereotype.Component

@Component
class GoboQueryResolver(private val goboInstrumentation: GoboInstrumentation) : Query {

    private val startTime = Instant.now()

    fun gobo(): Gobo {
        val fields = goboInstrumentation.fieldUsage.fields.map { GoboField(it.key, it.value.sum()) }
        val users = goboInstrumentation.userUsage.users.map { GoboUser(it.key, it.value.sum()) }
        return Gobo(startTime, GoboUsage(fields, users))
    }
}
