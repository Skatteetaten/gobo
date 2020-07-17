package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import com.expediagroup.graphql.spring.operations.Query
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.AuroraIntegration
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType
import no.skatteetaten.aurora.gobo.integration.cantus.Tag
import no.skatteetaten.aurora.gobo.integration.cantus.TagsDto
import no.skatteetaten.aurora.gobo.load
import no.skatteetaten.aurora.gobo.loadOptional
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.pageEdges
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.apache.commons.text.StringSubstitutor
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class ImageRepositoryQueryResolver : Query {

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
) : Query {

    fun guiUrl(
        imageRepository: ImageRepository,
        dfe: DataFetchingEnvironment
    ): String? {
        logger.debug {
            "Trying to find guiUrl for $imageRepository with configured repositories ${aurora.docker.values.map { it.url }
                .joinToString { "," }}"
        }
        val replacer =
            StringSubstitutor(mapOf("group" to imageRepository.namespace, "name" to imageRepository.name), "@", "@")
        return aurora.docker.values.find { it.url == imageRepository.registryUrl }?.let {
            replacer.replace(it.guiUrlPattern)
        }
    }

    fun tag(
        imageRepository: ImageRepository,
        names: List<String>,
        dfe: DataFetchingEnvironment
    ): List<ImageWithType> {

        if (!imageRepository.isFullyQualified) {
            return emptyList()
        }

        return names.map { name ->
            runBlocking {
                val image = dfe.load<ImageTag, Image>(ImageTag(imageRepository, name))
                ImageWithType(name, image)
            }
        }
    }

    fun <A> List<CompletableFuture<A>>.join(): CompletableFuture<List<A>> {
        return CompletableFuture.allOf(*this.toTypedArray()).thenApply {
            this.map { it.join() }
        }
    }

    suspend fun tags(
        imageRepository: ImageRepository,
        types: List<ImageTagType>?,
        filter: String?,
        first: Int? = null,
        after: String? = null,
        dfe: DataFetchingEnvironment
    ): ImageTagsConnection {

        val tagsDto: TagsDto = if (!imageRepository.isFullyQualified) {
            TagsDto(emptyList())
        } else {
            dfe.load(imageRepository.toImageRepo(filter))
        }

        val imageTags = tagsDto.tags.toImageTags(imageRepository, types)
        val allEdges = imageTags.map { ImageTagEdge(it) }
        return ImageTagsConnection(pageEdges(allEdges, first, after))
    }

    fun List<Tag>.toImageTags(imageRepository: ImageRepository, types: List<ImageTagType>?) = this
        .map { ImageTag(imageRepository = imageRepository, name = it.name) }
        .filter { types == null || it.type in types }
}

@Component
class ImageRepositoryTagResolver : Query {

    suspend fun image(imageTag: ImageTag, dfe: DataFetchingEnvironment): DataFetcherResult<Image?> {
        if (!imageTag.imageRepository.isFullyQualified) {
            return DataFetcherResult.newResult<Image?>().build()
        }
        return dfe.loadOptional(imageTag)
    }
}
