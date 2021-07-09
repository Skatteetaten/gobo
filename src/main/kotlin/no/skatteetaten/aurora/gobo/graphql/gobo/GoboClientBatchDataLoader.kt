package no.skatteetaten.aurora.gobo.graphql.gobo

import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.infrastructure.client.ClientService
import org.springframework.stereotype.Component

@Component
class GoboClientBatchDataLoader(private val clientService: ClientService) : GoboDataLoader<String, List<GoboClient>>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GoboGraphQLContext): Map<String, List<GoboClient>> {
        return keys.associateWith { name ->
            val clients = if (name.isEmpty()) {
                clientService.getAllClients()
            } else {
                clientService.getClientWithName(name)
            }

            clients.map { GoboClient(it.name, it.count) }
        }
    }
}
