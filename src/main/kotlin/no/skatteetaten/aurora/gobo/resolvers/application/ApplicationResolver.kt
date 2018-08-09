package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.application.ImageRegistryService
import no.skatteetaten.aurora.gobo.application.ImageRepo
import no.skatteetaten.aurora.gobo.resolvers.pageEdges
import org.springframework.stereotype.Component

@Component
class ApplicationResolver(
    val imageRegistryService: ImageRegistryService
) :
    GraphQLResolver<Application> {

    fun tags(
        application: Application,
        types: List<ImageTagType>?,
        first: Int? = null,
        after: String? = null
    ): ImageTagsConnection {

        val imageRepoString = application.applicationInstances.firstOrNull()?.details?.imageDetails?.dockerImageRepo
            ?: return ImageTagsConnection(emptyList(), null)

        val imageRepo = ImageRepo.fromRepoString(imageRepoString)
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