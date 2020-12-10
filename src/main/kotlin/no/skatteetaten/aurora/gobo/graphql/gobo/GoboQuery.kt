package no.skatteetaten.aurora.gobo.graphql.gobo

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.domain.ClientService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component
import java.time.Instant
import no.skatteetaten.aurora.gobo.domain.FieldService

@Component
class GoboQuery(
    private val fieldService: FieldService,
    private val clientService: ClientService
) : Query {

    private val startTime = Instant.now()

    fun gobo(dfe: DataFetchingEnvironment): Gobo {
        dfe.checkValidUserToken()
        return Gobo(startTime, GoboUsage(getFields(), getClients()))
    }

    private fun getFields(): List<GoboFieldUsage> {
        val fields = fieldService.getAllFields()
        return fields.map { field ->
            val clients = field.clients.map { GoboClient(it.name, it.count) }
            GoboFieldUsage(field.name, field.count, clients)
        }
    }

    private fun getClients(): List<GoboClient> {
        val clients = clientService.getAllClients()
        return clients.map { GoboClient(it.name, it.count) }
    }
}
