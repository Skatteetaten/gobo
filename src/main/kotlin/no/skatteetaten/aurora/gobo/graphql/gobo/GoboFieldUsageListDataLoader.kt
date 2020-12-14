package no.skatteetaten.aurora.gobo.graphql.gobo

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldService
import org.springframework.stereotype.Component

@Component
class GoboFieldUsageListDataLoader(private val fieldService: FieldService) :
    KeyDataLoader<String, List<GoboFieldUsage>> {
    override suspend fun getByKey(key: String, context: GoboGraphQLContext): List<GoboFieldUsage> {
        val fields = if (key.isEmpty()) {
            fieldService.getAllFields()
        } else {
            fieldService.getFieldWithName(key)
        }

        return fields.map { field ->
            GoboFieldUsage(
                field.name,
                field.count,
                field.clients.map { client ->
                    GoboClient(client.name, client.count)
                }
            )
        }
    }
}
