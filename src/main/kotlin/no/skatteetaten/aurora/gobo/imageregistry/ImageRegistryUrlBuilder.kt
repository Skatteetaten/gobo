package no.skatteetaten.aurora.gobo.imageregistry

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

interface ImageRegistryUrlBuilder {
    fun createManifestsUrl(imageRepo: ImageRepo, tag: String): String

    fun createTagListUrl(imageRepo: ImageRepo): String

    fun createApiUrl(imageRepo: ImageRepo): String
}

@Component
class DefaultImageRegistryUrlBuilder : ImageRegistryUrlBuilder {

    override fun createManifestsUrl(imageRepo: ImageRepo, tag: String) = "${createApiUrl(imageRepo)}/manifests/$tag"

    override fun createTagListUrl(imageRepo: ImageRepo) = "${createApiUrl(imageRepo)}/tags/list"

    override fun createApiUrl(imageRepo: ImageRepo): String {
        return if (imageRepo.registryUrl.startsWith("172")) {
            "http://${imageRepo.registryUrl}/v2/${imageRepo.namespace}/${imageRepo.name}"
        } else {
            "https://${imageRepo.registryUrl}/v2/${imageRepo.namespace}/${imageRepo.name}"
        }
    }
}

@Component
@Primary
@ConditionalOnProperty("docker-registry.url")
/**
 * This component is convenient to use if you need to override the registry that is used in the imageRepo parameter
 * with a hard coded one. For instance during development if the acutal image registry is not available, but the image
 * may be found in a test/dev registry.
 */
class OverrideRegistryImageRegistryUrlBuilder(
    @Value("\${docker-registry.url}") val registryUrl: String
) : DefaultImageRegistryUrlBuilder() {

    override fun createApiUrl(imageRepo: ImageRepo): String {
        return "$registryUrl/v2/${imageRepo.namespace}/${imageRepo.name}"
    }
}