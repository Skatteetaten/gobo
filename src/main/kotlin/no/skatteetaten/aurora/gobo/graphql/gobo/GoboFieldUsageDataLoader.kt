package no.skatteetaten.aurora.gobo.graphql.gobo

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldService
import org.springframework.stereotype.Component

data class GoboFieldUsageKey(val nameContains: String?, val mostUsedOnly: Boolean)

@Component
class GoboFieldUsageDataLoader(
    private val fieldService: FieldService?
) : GoboDataLoader<GoboFieldUsageKey, List<GoboFieldUsage>>() {
    override suspend fun getByKeys(
        keys: Set<GoboFieldUsageKey>,
        ctx: GraphQLContext
    ): Map<GoboFieldUsageKey, List<GoboFieldUsage>> {
        return keys.associateWith { key ->
            val fields = when {
                key.nameContains.isNullOrEmpty() -> fieldService?.getAllFields() ?: emptyList()
                else -> fieldService?.getFieldWithName(key.nameContains) ?: emptyList()
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
