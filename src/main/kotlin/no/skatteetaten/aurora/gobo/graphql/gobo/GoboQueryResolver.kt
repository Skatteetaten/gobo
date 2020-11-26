package no.skatteetaten.aurora.gobo.graphql.gobo

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.GoboInstrumentation
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class GoboQueryResolver(private val goboInstrumentation: GoboInstrumentation) : Query {

    private val startTime = Instant.now()

    fun gobo(dfe: DataFetchingEnvironment): Gobo {
        dfe.checkValidUserToken()
//        val fields = goboInstrumentation.fieldUsage.fields.map { GoboField(it.key, it.value.sum()) }
        val fields = goboInstrumentation.fieldUsage.fields.map { GoboFieldCounter(it.key, it.value.sum()) }
        val users = goboInstrumentation.userUsage.users.map { GoboUser(it.key, it.value.sum()) }
        return Gobo(startTime, GoboUsage(fields, users))
    }
}
