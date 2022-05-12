package no.skatteetaten.aurora.gobo.graphql.storagegrid

import org.springframework.stereotype.Component
import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.mokey.StoragegridObjectAreasService

@Component
class StoragegridObjectAreaDataLoader(
    val storagegridObjectareaService: StoragegridObjectAreasService,
) : GoboDataLoader<String, List<StoragegridObjectArea>>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GraphQLContext): Map<String, List<StoragegridObjectArea>> {
        return keys.associateWith { affiliation ->
            storagegridObjectareaService.getObjectAreas(affiliation, ctx.token)
                .map { StoragegridObjectArea.fromResource(it) }
        }
    }
}
