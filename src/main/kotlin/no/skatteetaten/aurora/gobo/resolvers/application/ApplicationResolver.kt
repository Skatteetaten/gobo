package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.integration.imageregistry.RegistryMetadataResolver
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import org.springframework.stereotype.Component

@Component
class ApplicationResolver(private val registryMetadataResolver: RegistryMetadataResolver) : GraphQLResolver<Application> {

    fun imageRepository(application: Application): ImageRepository? {
        return application.applicationDeployments
                .asSequence()
                .mapNotNull { it.details?.imageDetails?.dockerImageRepo }
                .map { registryMetadataResolver.getMetadataForRegistry(it) }
                .firstOrNull { !it.isInternal }
                ?.let { ImageRepository.fromRepoString(it.registry) }
    }
}