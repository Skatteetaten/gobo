package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.application.ImageRegistryService
import no.skatteetaten.aurora.gobo.application.ImageRepo
import no.skatteetaten.aurora.gobo.resolvers.pageEdges
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

        val imageRepo = ImageRepo.fromRepoString(imageRepository.repository)
        val tagsInRepo = try {
            imageRegistryService.findTagNamesInRepo(imageRepo)
        } catch (e: Exception) {
            emptyList<String>()
        }
        val matchingTags = tagsInRepo
            .map { ImageTag(imageRepo, it) }
            .filter { types == null || it.type in types }

        val allEdges = matchingTags.map { ImageTagEdge(it) }
        return ImageTagsConnection(pageEdges(allEdges, first, after))
    }
}