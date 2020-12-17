package no.skatteetaten.aurora.gobo.graphql.gobo

import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldService
import org.springframework.stereotype.Component

data class GoboFieldCount(val numberOfFields: Long)

@Component
class GoboFieldCountDataLoader(private val fieldService: FieldService) : KeyDataLoader<GoboUsage, GoboFieldCount> {
    override suspend fun getByKey(key: GoboUsage, context: GoboGraphQLContext) =
        GoboFieldCount(fieldService.getFieldCount())
}
