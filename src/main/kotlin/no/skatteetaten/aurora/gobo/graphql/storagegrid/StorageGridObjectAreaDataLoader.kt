package no.skatteetaten.aurora.gobo.graphql.storagegrid

import org.springframework.stereotype.Component
import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.mokey.StorageGridObjectAreasService

@Component
class StorageGridObjectAreaDataLoader(
    val storageGridObjectareaService: StorageGridObjectAreasService,
) : GoboDataLoader<String, List<StorageGridObjectArea>>() {
    override suspend fun getByKeys(keys: Set<String>, ctx: GraphQLContext): Map<String, List<StorageGridObjectArea>> {
        return keys.associateWith { affiliation ->
            storageGridObjectareaService.getObjectAreas(affiliation, ctx.token)
                .map { StorageGridObjectArea.fromResource(it) }
        }
    }
}
