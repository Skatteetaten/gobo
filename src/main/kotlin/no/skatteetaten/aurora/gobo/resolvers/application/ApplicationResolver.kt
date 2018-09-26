package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.integration.imageregistry.RegistryMetadataResolver
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import org.springframework.stereotype.Component

@Component
class ApplicationResolver(private val registryMetadataResolver: RegistryMetadataResolver) :
    GraphQLResolver<Application> {

    fun imageRepository(application: Application): ImageRepository? = application.imageRepository

    val Application.imageRepository
        /**
         * Gets the first repository from all the deployments of the given application that is not located in an
         * internal OpenShift cluster. The assumption is that this will be the "correct" repository for the application.
         */
        get(): ImageRepository? = this.applicationDeployments.asSequence()
            .mapNotNull { it.details?.imageDetails?.dockerImageRepo }
            .map { ImageRepository.fromRepoString(it) }
            .firstOrNull {
                val metadata = registryMetadataResolver.getMetadataForRegistry(it.registryUrl)
                !metadata.isInternal
            }
}