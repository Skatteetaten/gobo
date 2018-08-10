package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.application.ImageRegistryService
import no.skatteetaten.aurora.gobo.application.ImageRepo
import org.springframework.stereotype.Component

@Component
class ApplicationResolver(val imageRegistryService: ImageRegistryService) : GraphQLResolver<Application> {

    fun imageRepository(application: Application): ImageRepository? {

        val imageRepoString = application.applicationInstances.firstOrNull()?.details?.imageDetails?.dockerImageRepo
            ?: return null

        return ImageRepo.fromRepoString(imageRepoString).let {
            ImageRepository(it.registryUrl, it.namespace, it.name)
        }
    }
}