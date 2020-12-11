package no.skatteetaten.aurora.gobo.graphql.gobo

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.infrastructure.client.ClientService
import org.springframework.stereotype.Component

@Component
class GoboClientListDataLoader(private val clientService: ClientService) : KeyDataLoader<GoboUsage, List<GoboClient>> {
    override suspend fun getByKey(key: GoboUsage, context: GoboGraphQLContext) =
        clientService.getAllClients().map { GoboClient(it.name, it.count) }
}
