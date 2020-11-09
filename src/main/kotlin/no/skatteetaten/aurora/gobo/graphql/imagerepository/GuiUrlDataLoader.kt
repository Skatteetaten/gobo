package no.skatteetaten.aurora.gobo.graphql.imagerepository

import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.AuroraIntegration
import no.skatteetaten.aurora.gobo.KeyDataLoader
import no.skatteetaten.aurora.gobo.graphql.GoboGraphQLContext
import org.apache.commons.text.StringSubstitutor
import org.springframework.stereotype.Component

data class GuiUrl(val url: String?)

private val logger = KotlinLogging.logger {}

@Component
class GuiUrlDataLoader(private val aurora: AuroraIntegration) : KeyDataLoader<ImageRepository, GuiUrl> {
    override suspend fun getByKey(key: ImageRepository, context: GoboGraphQLContext): GuiUrl {
        logger.debug {
            "Trying to find guiUrl for $key with configured repositories ${
            aurora.docker.values.map { it.url }.joinToString { "," }
            }"
        }
        val replacer =
            StringSubstitutor(mapOf("group" to key.namespace, "name" to key.name), "@", "@")
        val url = aurora.docker.values.find { it.url == key.registryUrl }?.let {
            replacer.replace(it.guiUrlPattern)
        }
        return GuiUrl(url)
    }
}
