package no.skatteetaten.aurora.gobo.graphql.gobo

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.infrastructure.client.ClientService
import org.springframework.stereotype.Component

data class GoboClientCount(val numberOfClients: Long)

@Component
class GoboClientCountDataLoader(private val clientService: ClientService) : KeyDataLoader<GoboUsage, GoboClientCount> {
    override suspend fun getByKey(key: GoboUsage, context: GoboGraphQLContext) =
        GoboClientCount(clientService.getClientCount())
}
