package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.resolvers.pageEdges
import no.skatteetaten.aurora.gobo.service.imageregistry.ImageRegistryService
import org.springframework.stereotype.Component

@Component
class ImageRepositoryResolver(
    val imageRegistryService: ImageRegistryService
) :
    GraphQLResolver<ImageRepository> {

    fun tags(
        imageRepository: ImageRepository,
        types: List<ImageTagType>?,
        first: Int? = null,
        after: String? = null
    ): ImageTagsConnection {

        val tagsInRepo = try {
            imageRegistryService.findTagNamesInRepoOrderedByCreatedDateDesc(toImageRepo(imageRepository))
        } catch (e: Exception) {
            emptyList<String>()
        }
        val matchingTags = tagsInRepo
            .map { ImageTag(imageRepository, it) }
            .filter { types == null || it.type in types }

        val allEdges = matchingTags.map { ImageTagEdge(it) }
        return ImageTagsConnection(pageEdges(allEdges, first, after))
    }
}