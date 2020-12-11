package no.skatteetaten.aurora.gobo.graphql.gobo

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldService
import org.springframework.stereotype.Component

@Component
class GoboFieldUsageListDataLoader(private val fieldService: FieldService) : KeyDataLoader<GoboUsage, List<GoboFieldUsage>> {
    override suspend fun getByKey(key: GoboUsage, context: GoboGraphQLContext): List<GoboFieldUsage> =
        fieldService.getAllFields().map { field ->
            GoboFieldUsage(
                field.name,
                field.count,
                field.clients.map { client ->
                    GoboClient(client.name, client.count)
                }
            )
        }
}
