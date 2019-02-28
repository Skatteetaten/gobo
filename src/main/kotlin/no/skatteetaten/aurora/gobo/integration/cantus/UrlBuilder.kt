package no.skatteetaten.aurora.gobo.integration.cantus

import org.springframework.stereotype.Component

@Component
class UrlBuilder {

    fun createTagsUrl(
        imageRepoDto: ImageRepoDto
    ): String {
        val path = "/{namespace}/{name}/tags"
        return imageRepoDto.addOverrideToPathIfExists(path)
    }

    fun createImageTagUrl(
        imageRepoDto: ImageRepoDto
    ): String {
        val path = "/{namespace}/{name}/{tag}/manifest"
        return imageRepoDto.addOverrideToPathIfExists(path)
    }

    fun ImageRepoDto.addOverrideToPathIfExists(path: String) =
        if (this.registry.isEmpty()) path
        else "$path?dockerRegistryUrl=${this.registry}"
}