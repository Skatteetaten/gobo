package no.skatteetaten.aurora.gobo.resolvers.application

import com.coxautodev.graphql.tools.GraphQLResolver
import no.skatteetaten.aurora.gobo.application.DockerRegistryService
import org.springframework.stereotype.Component

@Component
class ApplicationResolver(val dockerRegistryService: DockerRegistryService) : GraphQLResolver<Application> {

    fun tags(application: Application): List<String> {
        val dockerImageReference = application.applicationInstances.first().details?.imageDetails?.dockerImageReference ?: ""
        val dockerImageName = dockerImageReference.replace(Regex("@.*$"), "").replace(Regex("^.*5000/"), "")

        val allTagsFor = dockerRegistryService.findAllTagsFor(dockerImageName)
        return allTagsFor.map { it.name }
    }
}