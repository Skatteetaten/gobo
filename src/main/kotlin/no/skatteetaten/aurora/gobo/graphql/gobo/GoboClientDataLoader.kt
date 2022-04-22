package no.skatteetaten.aurora.gobo.graphql.gobo

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.infrastructure.client.ClientService
import org.springframework.stereotype.Component

@Component
class GoboClientDataLoader(private val clientService: ClientService? = null) : GoboDataLoader<String, List<GoboClient>>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GraphQLContext): Map<String, List<GoboClient>> {
        return keys.associateWith { name ->
            val clients = if (name.isEmpty()) {
                clientService?.getAllClients() ?: emptyList()
            } else {
                clientService?.getClientWithName(name) ?: emptyList()
            }

            clients.map { GoboClient(it.name, it.count) }
        }
    }
}
