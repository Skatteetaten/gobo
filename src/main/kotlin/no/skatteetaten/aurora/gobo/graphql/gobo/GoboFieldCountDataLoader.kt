package no.skatteetaten.aurora.gobo.graphql.gobo

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldService
import org.springframework.stereotype.Component

@Component
class GoboFieldCountDataLoader(private val fieldService: FieldService?) : GoboDataLoader<GoboUsage, Long>() {
    override suspend fun getByKeys(keys: Set<GoboUsage>, ctx: GraphQLContext): Map<GoboUsage, Long> {
        return keys.associateWith { fieldService?.getFieldCount() ?: 0 }
    }
}
