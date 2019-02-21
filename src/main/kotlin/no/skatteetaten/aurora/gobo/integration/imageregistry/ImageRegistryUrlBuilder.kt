package no.skatteetaten.aurora.gobo.integration.imageregistry

import org.slf4j.LoggerFactory
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

@Component
@Primary
@ConditionalOnProperty("integrations.docker-registry.url")
/**
 * This component is convenient to use if you need to override the registry that is used in the imageRepoMetadata parameter
 * with a hard coded one. For instance during development if the acutal image registry is not available, but the image
 * may be found in a test/dev registry.
 */
class OverrideRegistryImageRegistryUrlBuilder(
    @Value("\${integrations.docker-registry.url}") val registryUrl: String
) : ImageRegistryUrlBuilder() {

    private val logger = LoggerFactory.getLogger(OverrideRegistryImageRegistryUrlBuilder::class.java)

    init {
        logger.info("Override docker registry with url=$registryUrl")
    }

    override fun createApiUrl(apiSchema: String, imageRepoDto: ImageRepoDto): String =
        "$registryUrl/v2/${imageRepoDto.namespace}/${imageRepoDto.name}"
}