package no.skatteetaten.aurora.gobo.graphql.gobo

import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldService
import org.springframework.stereotype.Component

data class GoboFieldUsageKey(val nameContains: String?, val mostUsedOnly: Boolean)

@Component
class GoboFieldUsageDataLoader(
    private val fieldService: FieldService
) : GoboDataLoader<GoboFieldUsageKey, List<GoboFieldUsage>>() {
    override suspend fun getByKeys(
        keys: Set<GoboFieldUsageKey>,
        ctx: GoboGraphQLContext
    ): Map<GoboFieldUsageKey, List<GoboFieldUsage>> {
        return keys.associateWith { key ->
            val fields = when {
                key.nameContains.isNullOrEmpty() -> fieldService.getAllFields()
                else -> fieldService.getFieldWithName(key.nameContains)
            }.map { field ->
                GoboFieldUsage(
                    field.name,
                    field.count,
                    field.clients.map { client ->
                        GoboClient(client.name, client.count)
                    }
                )
            }

            when {
                key.mostUsedOnly -> fields.sortedByDescending { it.count }.take(5)
                else -> fields
            }
        }
    }
}
