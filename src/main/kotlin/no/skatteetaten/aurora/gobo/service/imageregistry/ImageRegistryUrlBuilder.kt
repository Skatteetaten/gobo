package no.skatteetaten.aurora.gobo.service.imageregistry

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
class ImageRegistryUrlBuilder {

    fun createManifestsUrl(apiSchema: String, imageRepo: ImageRepo, tag: String) =
        "${createApiUrl(apiSchema, imageRepo)}/manifests/$tag"

    fun createTagListUrl(apiSchema: String, imageRepo: ImageRepo) = "${createApiUrl(apiSchema, imageRepo)}/tags/list"

    fun createApiUrl(apiSchema: String, imageRepo: ImageRepo): String {
        val registryAddress = imageRepo.registry
        val namespace = imageRepo.namespace
        val name = imageRepo.name
        return "$apiSchema://$registryAddress/v2/$namespace/$name"
    }
}

@Component
@Primary
@ConditionalOnProperty("docker-registry.url")
/**
 * This component is convenient to use if you need to override the registry that is used in the imageRepoMetadata parameter
 * with a hard coded one. For instance during development if the acutal image registry is not available, but the image
 * may be found in a test/dev registry.
 */
class OverrideRegistryImageRegistryUrlBuilder(
    @Value("\${docker-registry.url}") val registryUrl: String
) : ImageRegistryUrlBuilder() {

    override fun createApiUrl(apiSchema: String, imageRepo: ImageRepo): String =
        "$registryUrl/v2/${imageRepo.namespace}/${imageRepo.name}"
}