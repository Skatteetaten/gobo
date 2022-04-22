package no.skatteetaten.aurora.gobo.graphql.imagerepository

import graphql.GraphQLContext
import no.skatteetaten.aurora.gobo.graphql.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.token
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.cantus.Version
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class VersionDataLoader(private val imageRegistryService: ImageRegistryService) : GoboDataLoader<ImageTag, Image?>() {

    override suspend fun getByKeys(keys: Set<ImageTag>, ctx: GraphQLContext): Map<ImageTag, Image?> {

        val versions: List<Version> = keys
            .groupBy { Pair(it.imageRepository.namespace, it.imageRepository.name) }
            .keys
            .flatMap { (namespace, name) -> imageRegistryService.findVersions(namespace, name, ctx.token) }

        return keys.associateWith { key ->
            versions.find { it.name == key.name }?.let {
                Image(
                    ZonedDateTime.parse(it.lastModified, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant(),
                    it.name
                )
            }
        }
    }
}
