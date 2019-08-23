package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.AuroraIntegration
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType
import no.skatteetaten.aurora.gobo.integration.cantus.Tag
import no.skatteetaten.aurora.gobo.integration.cantus.decomposeToImageRepoSegments
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.resolvers.multipleKeysLoader
import no.skatteetaten.aurora.gobo.resolvers.pageEdges
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.apache.commons.text.StringSubstitutor
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class ImageRepositoryQueryResolver : GraphQLQueryResolver {

    fun getImageRepositories(repositories: List<String>, dfe: DataFetchingEnvironment): List<ImageRepository> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot access imagrepositories")
        if (repositories.isEmpty()) throw GoboException("repositories is empty")

        return repositories.map { ImageRepository.fromRepoString(it) }
    }
}

@Component
class ImageRepositoryResolver(
    val imageRegistryServiceBlocking: ImageRegistryServiceBlocking,
    val aurora: AuroraIntegration
) : GraphQLResolver<ImageRepository> {

    fun guiUrl(
        imageRepository: ImageRepository,
        dfe: DataFetchingEnvironment
    ): String? {
        val replacer =
            StringSubstitutor(mapOf("group" to imageRepository.namespace, "name" to imageRepository.name), "@", "@")
        return aurora.docker.values.find { it.url == imageRepository.registryUrl }?.let {
            replacer.replace(it.guiUrlPattern)
        }
    }

    fun tags(
        imageRepository: ImageRepository,
        types: List<ImageTagType>?,
        first: Int? = null,
        after: String? = null,
        dfe: DataFetchingEnvironment
    ) =
        dfe.loader(ImageTagListDataLoader::class).load(imageRepository.toImageRepo())
            .thenApply { dto ->
                val imageTags = dto.tags.toImageTags(imageRepository, types)
                val allEdges = imageTags.map { ImageTagEdge(it) }
                ImageTagsConnection(pageEdges(allEdges, first, after))
            }

    fun List<Tag>.toImageTags(imageRepository: ImageRepository, types: List<ImageTagType>?) = this
        .map { ImageTag(imageRepository = imageRepository, name = it.name) }
        .filter { types == null || it.type in types }
}

@Component
class ImageRepositoryTagResolver : GraphQLResolver<ImageTag> {

    fun image(imageTag: ImageTag, dfe: DataFetchingEnvironment): CompletableFuture<Image?> =
        dfe.multipleKeysLoader(ImageTagDataLoader::class).load(imageTag)
}