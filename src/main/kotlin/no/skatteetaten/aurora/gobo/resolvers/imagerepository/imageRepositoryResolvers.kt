package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.GoboException
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagType
import no.skatteetaten.aurora.gobo.integration.cantus.Tag
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.resolvers.multipleKeysLoader
import no.skatteetaten.aurora.gobo.resolvers.pageEdges
import no.skatteetaten.aurora.gobo.security.currentUser
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
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
class ImageRepositoryResolver(val imageRegistryServiceBlocking: ImageRegistryServiceBlocking) :
    GraphQLResolver<ImageRepository> {

    fun tags(
        imageRepository: ImageRepository,
        types: List<ImageTagType>?,
        first: Int? = null,
        after: String? = null,
        dfe: DataFetchingEnvironment
    ): ImageTagsConnection {
        if (isNonFilteredRequest(dfe.arguments)) {
            throw GoboException("Requests for tags without 'types' and 'first' filters are not allowed")
        }

        val imageTags =
            imageRegistryServiceBlocking.findTagNamesInRepoOrderedByCreatedDateDesc(
                imageRepoDto = imageRepository.toImageRepo(),
                token = dfe.currentUser().token
            ).tags.toImageTags(imageRepository, types)

        val allEdges = imageTags.map { ImageTagEdge(it) }

        return ImageTagsConnection(pageEdges(allEdges, first, after))
    }

    private fun isNonFilteredRequest(arguments: MutableMap<String, Any>): Boolean {
        return when {
            arguments["types"] == null -> true
            arguments["first"] == null -> true
            else -> false
        }
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