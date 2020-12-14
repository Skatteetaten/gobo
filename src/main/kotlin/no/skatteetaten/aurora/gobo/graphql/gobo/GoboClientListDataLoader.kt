package no.skatteetaten.aurora.gobo.graphql.gobo

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.infrastructure.client.ClientService
import org.springframework.stereotype.Component

@Component
class GoboClientListDataLoader(private val clientService: ClientService) : KeyDataLoader<String, List<GoboClient>> {
    override suspend fun getByKey(key: String, context: GoboGraphQLContext): List<GoboClient> {
        val clients = if (key.isEmpty()) {
            clientService.getAllClients()
        } else {
            clientService.getClientWithName(key)
        }

        return clients.map { GoboClient(it.name, it.count) }
    }
}
