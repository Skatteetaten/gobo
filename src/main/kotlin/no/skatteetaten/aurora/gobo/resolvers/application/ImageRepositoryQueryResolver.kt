package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import no.skatteetaten.aurora.gobo.application.ImageRegistryService
import no.skatteetaten.aurora.gobo.application.ImageRepo
import org.springframework.stereotype.Component

@Component
class ImageRepositoryQueryResolver(private val imageRegistryService: ImageRegistryService) : GraphQLQueryResolver {

    fun getImageRepositories(repositories: List<String>): List<ImageRepository> {

        return repositories.map {
            val imageRepo = ImageRepo.fromRepoString(it)
            ImageRepository(imageRepo.registryUrl, imageRepo.namespace, imageRepo.name)
        }
    }
}