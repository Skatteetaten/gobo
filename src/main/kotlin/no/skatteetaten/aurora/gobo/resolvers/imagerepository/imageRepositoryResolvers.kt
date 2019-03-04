package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import mu.KotlinLogging
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType
import no.skatteetaten.aurora.gobo.integration.cantus.TagsDto
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.resolvers.multipleKeysLoader
import no.skatteetaten.aurora.gobo.resolvers.pageEdges
import no.skatteetaten.aurora.gobo.security.currentUser
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Component
class ImageRepositoryQueryResolver : GraphQLQueryResolver {

    fun getImageRepositories(repositories: List<String>, dfe: DataFetchingEnvironment): List<ImageRepository> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot access imagrepositories")
        return repositories.map { ImageRepository.fromRepoString(it) }
    }
}

private val logger = KotlinLogging.logger { }

@Component
class ImageRepositoryResolver(val imageRegistryServiceBlocking: ImageRegistryServiceBlocking) :
    GraphQLResolver<ImageRepository> {

    fun tags(
        imageRepository: ImageRepository,
        types: List<ImageTagType>?,
        first: Int? = null,
        after: String? = null,
        dfe: DataFetchingEnvironment
    ): ImageTagsConnection {

        val tagsInRepo = try {
            imageRegistryServiceBlocking.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepository.toImageRepo(), dfe.currentUser().token)
        } catch (e: Exception) {
            logger.warn(e) {
                "Exception occurred in method=findTagNamesInRepoOrderedByCreatedDateDesc with input=${imageRepository.toImageRepo()}"
            }
            TagsDto(emptyList())
        }
        val matchingTags = tagsInRepo.tags
            .map {
                ImageTag(
                    imageRepository = imageRepository,
                    name = it.name
                )
            }
            .filter { types == null || it.type in types }

        val allEdges = matchingTags.map { ImageTagEdge(it) }
        return ImageTagsConnection(pageEdges(allEdges, first, after))
    }
}

@Component
class ImageRepositoryTagResolver : GraphQLResolver<ImageTag> {

    fun lastModified(imageTag: ImageTag, dfe: DataFetchingEnvironment): CompletableFuture<Instant> =
        dfe.multipleKeysLoader(ImageTagDataLoader::class).load(imageTag)
}