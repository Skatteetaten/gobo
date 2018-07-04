package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.application.ImageRegistryService
import no.skatteetaten.aurora.gobo.application.ImageRepo
import org.springframework.stereotype.Component

@Component
class ApplicationResolver(val imageRegistryService: ImageRegistryService) : GraphQLResolver<Application> {

    fun tags(application: Application): List<String> {
        val imageRepo = application.applicationInstances.firstOrNull()?.details?.imageDetails?.dockerImageRepo
            ?: return emptyList()

        val allTagsFor = imageRegistryService.findAllTagsInRepo(ImageRepo.fromRepoString(imageRepo))
        return allTagsFor.map { it.name }
    }
}