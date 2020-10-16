package no.skatteetaten.aurora.gobo.graphql.gobo

//import com.expediagroup.graphql.spring.operations.Query
//import org.springframework.stereotype.Component
//import java.time.Instant
//
//@Component
//class GoboQueryResolver(private val goboInstrumentation: GoboInstrumentation) : Query {
//
//    private val startTime = Instant.now()
//
//    fun gobo(): Gobo {
//        val fields = goboInstrumentation.fieldUsage.fields.map { GoboField(it.key, it.value.sum()) }
//        val users = goboInstrumentation.userUsage.users.map { GoboUser(it.key, it.value.sum()) }
//        return Gobo(startTime, GoboUsage(fields, users))
//    }
//}
