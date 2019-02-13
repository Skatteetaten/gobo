package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.imageregistry.ImageTagType
import no.skatteetaten.aurora.gobo.integration.imageregistry.TagsDto
import no.skatteetaten.aurora.gobo.resolvers.EmptyResponseException
import no.skatteetaten.aurora.gobo.resolvers.loader
import no.skatteetaten.aurora.gobo.resolvers.pageEdges
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

@Component
class ImageRepositoryQueryResolver : GraphQLQueryResolver {

    fun getImageRepositories(repositories: List<String>) =
        repositories.map { ImageRepository.fromRepoString(it) }
}

@Component
class ImageRepositoryResolver(val imageRegistryServiceBlocking: ImageRegistryServiceBlocking) :
    GraphQLResolver<ImageRepository> {

    fun tags(
        imageRepository: ImageRepository,
        types: List<ImageTagType>?,
        first: Int? = null,
        after: String? = null
    ): ImageTagsConnection {

        val tagsInRepo = try {
            imageRegistryServiceBlocking.findTagNamesInRepoOrderedByCreatedDateDesc(imageRepository.toImageRepo(""))
        } catch (e: Exception) {
            // TODO: Indicate error to caller
            throw e
        }
        val matchingTags = tagsInRepo.tags
            .map { ImageTag(
                imageRepository = imageRepository,
                name = it.name,
                type = it.type
            ) }
            .filter { types == null || it.type in types }

        val allEdges = matchingTags.map { ImageTagEdge(it) }
        return ImageTagsConnection(pageEdges(allEdges, first, after))
    }
}

@Component
class ImageRepositoryTagResolver : GraphQLResolver<ImageTag> {

    fun lastModified(imageTag: ImageTag, dfe: DataFetchingEnvironment) {
        val request: HttpServletRequest = dfe.getContext()
        println(request.getHeader("Authorization"))
        dfe.loader(ImageTagDataLoader::class).load(imageTag)
    }
}