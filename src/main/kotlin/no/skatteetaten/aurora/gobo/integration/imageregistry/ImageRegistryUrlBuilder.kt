package no.skatteetaten.aurora.gobo.integration.imageregistry

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
class ImageRegistryUrlBuilder {

    fun createManifestsUrl(apiSchema: String, imageRepoDto: ImageRepoDto, tag: String) =
        "${createApiUrl(apiSchema, imageRepoDto)}/manifests/$tag"

    fun createTagListUrl(apiSchema: String, imageRepoDto: ImageRepoDto) =
        "${createApiUrl(apiSchema, imageRepoDto)}/tags/list"

    fun createApiUrl(apiSchema: String, imageRepoDto: ImageRepoDto): String {
        val registryAddress = imageRepoDto.registry
        val namespace = imageRepoDto.namespace
        val name = imageRepoDto.name
        return "$apiSchema://$registryAddress/v2/$namespace/$name"
    }
}

private val logger = KotlinLogging.logger {}

@Component
@Primary
@ConditionalOnProperty("gobo.docker-registry.url")
/**
 * This component is convenient to use if you need to override the registry that is used in the imageRepoMetadata parameter
 * with a hard coded one. For instance during development if the acutal image registry is not available, but the image
 * may be found in a test/dev registry.
 */
class OverrideRegistryImageRegistryUrlBuilder(
    @Value("\${gobo.docker-registry.url}") val registryUrl: String
) : ImageRegistryUrlBuilder() {

    init {
        logger.info("Override docker registry with url=$registryUrl")
    }

    override fun createApiUrl(apiSchema: String, imageRepoDto: ImageRepoDto): String =
        "$registryUrl/v2/${imageRepoDto.namespace}/${imageRepoDto.name}"
}