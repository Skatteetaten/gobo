package no.skatteetaten.aurora.gobo.graphql.gobo

import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldService
import org.springframework.stereotype.Component

@Component
class GoboFieldCountDataLoader(private val fieldService: FieldService) : GoboDataLoader<GoboUsage, Long>() {
    override suspend fun getByKeys(keys: Set<GoboUsage>, ctx: GoboGraphQLContext): Map<GoboUsage, Long> {
        return keys.associateWith { fieldService.getFieldCount() }
    }
}
