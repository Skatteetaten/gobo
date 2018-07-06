package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.application.ImageRegistryService
import no.skatteetaten.aurora.gobo.application.ImageRepo
import org.springframework.stereotype.Component

@Component
class ApplicationResolver(
    val imageRegistryService: ImageRegistryService
) :
    GraphQLResolver<Application> {

    fun tags(application: Application, types: List<ImageTagType>?): ImageTagsConnection {

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
        return ImageTagsConnection(matchingTags.map { ImageTagEdge(it) }, null)
    }
}