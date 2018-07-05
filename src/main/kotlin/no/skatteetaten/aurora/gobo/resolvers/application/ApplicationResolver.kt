package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.application.ImageRegistryService
import no.skatteetaten.aurora.gobo.application.ImageRepo
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ApplicationResolver(
    val imageRegistryService: ImageRegistryService
) :
    GraphQLResolver<Application> {

    fun tags(application: Application, types: List<ImageTagType>?): ImageTagsConnection {

        val imageRepo = application.applicationInstances.firstOrNull()?.details?.imageDetails?.dockerImageRepo
            ?: return ImageTagsConnection(emptyList(), null)

        val tagsInRepo = imageRegistryService.findAllTagsInRepo(ImageRepo.fromRepoString(imageRepo))
        val matchingTags = tagsInRepo.map { ImageTag(it.name, Instant.now()) }
            .filter { types == null || it.type in types }
        return ImageTagsConnection(matchingTags.map { ImageTagEdge(it) }, null)
    }
}