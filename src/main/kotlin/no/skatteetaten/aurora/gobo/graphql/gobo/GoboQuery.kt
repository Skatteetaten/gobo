package no.skatteetaten.aurora.gobo.graphql.gobo

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component
import java.time.Instant
import no.skatteetaten.aurora.gobo.domain.FieldService

@Component
class GoboQuery(private val fieldService: FieldService) : Query {

    private val startTime = Instant.now()

    fun gobo(dfe: DataFetchingEnvironment): Gobo {
        dfe.checkValidUserToken()

        val fields = fieldService.getAllFields()

        val fieldUsage = fields.map { field ->
            val clients = field.clients.map { GoboUser(it.name, it.count) }
            GoboFieldUsage(field.name, field.count, clients)
        }
        return Gobo(startTime, GoboUsage(fieldUsage, emptyList()))
    }
}
