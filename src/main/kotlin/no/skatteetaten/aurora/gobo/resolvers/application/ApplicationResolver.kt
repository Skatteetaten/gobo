package no.skatteetaten.aurora.gobo.resolvers.application

import com.expediagroup.graphql.spring.operations.Query
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ApplicationResolver(val dockerRegistry: DockerRegistry) :
    Query {

    fun imageRepository(application: Application): ImageRepository? = application.imageRepository

    val Application.imageRepository
        /**
         * Gets the first repository from all the deployments of the given application that is not located in an
         * internal OpenShift cluster. The assumption is that this will be the "correct" repository for the application.
         */
        get(): ImageRepository? = this.applicationDeployments.asSequence()
            .mapNotNull { it.dockerImageRepo }
            .map { ImageRepository.fromRepoString(it) }
            .firstOrNull { it.registryUrl != null && !dockerRegistry.isInternal(it.registryUrl) }
}

@Component
class DockerRegistry(@Value("\${integrations.internal-registry.url:docker-registry.default.svc:5000}") val internalRegistryAddress: String) {

    private val ipV4WithPortRegex =
        "^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]):([0-9]{1,4})(.*)\$".toRegex()

    fun isInternal(registry: String) =
        registry == internalRegistryAddress || registry.matches(ipV4WithPortRegex)
}
