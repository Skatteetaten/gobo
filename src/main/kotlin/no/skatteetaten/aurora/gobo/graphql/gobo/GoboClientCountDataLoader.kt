package no.skatteetaten.aurora.gobo.graphql.gobo

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.infrastructure.client.ClientService
import org.springframework.stereotype.Component

@Component
class GoboClientCountDataLoader(private val clientService: ClientService? = null) : GoboDataLoader<GoboUsage, Long>() {
    override suspend fun getByKeys(keys: Set<GoboUsage>, ctx: GraphQLContext): Map<GoboUsage, Long> {
        return keys.associateWith { clientService?.getClientCount() ?: 0 }
    }
}
