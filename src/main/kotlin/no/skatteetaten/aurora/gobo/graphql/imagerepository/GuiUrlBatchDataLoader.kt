package no.skatteetaten.aurora.gobo.graphql.imagerepository

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.AuroraIntegration
import no.skatteetaten.aurora.gobo.GoboDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import org.apache.commons.text.StringSubstitutor
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class GuiUrlBatchDataLoader(private val aurora: AuroraIntegration) : GoboDataLoader<ImageRepository, String?>() {
    override suspend fun getByKeys(keys: Set<ImageRepository>, ctx: GoboGraphQLContext): Map<ImageRepository, String?> {
        return keys.associateWith { imageRepository ->
            logger.debug {
                "Trying to find guiUrl for $imageRepository with configured repositories ${
                aurora.docker.values.map { it.url }.joinToString { "," }
                }"
            }
            val replacer =
                StringSubstitutor(mapOf("group" to imageRepository.namespace, "name" to imageRepository.name), "@", "@")
            aurora.docker.values.find { it.url == imageRepository.registryUrl }?.let {
                replacer.replace(it.guiUrlPattern)
            }
        }
    }
}
