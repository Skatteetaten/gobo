package no.skatteetaten.aurora.gobo.integration.imageregistry

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

val logger = LoggerFactory.getLogger(ImageRegistryUrlBuilder::class.java)

@Component
class ImageRegistryUrlBuilder {

    fun createTagsUrl(
        imageRepoDto: ImageRepoDto,
        registryMetadata: RegistryMetadata
    ): String {
        val path = "/{namespace}/{name}/tags"
        return imageRepoDto.addOverrideToPathIfExists(path)
    }

    fun createImageTagUrl(
        imageRepoDto: ImageRepoDto,
        registryMetadata: RegistryMetadata
    ): String {
        val path = "/{namespace}/{name}/{tag}/manifest"
        return imageRepoDto.addOverrideToPathIfExists(path)
    }

    fun ImageRepoDto.addOverrideToPathIfExists(path: String) =
        if (this.registry.isEmpty()) path
        else "$path?dockerRegistryUrl=${this.registry}"
}