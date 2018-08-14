package no.skatteetaten.aurora.gobo.resolvers.application

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.service.application.ApplicationInstanceDetailsResource
import no.skatteetaten.aurora.gobo.service.application.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.ApplicationInstance
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.createApplicationInstances
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository

data class Application(
    val name: String,
    val applicationInstances: List<ApplicationInstance>
) {
    val imageRepoString: ImageRepository?
        get() {
            return applicationInstances
                .firstOrNull { it.details?.imageDetails?.dockerImageRepo != null }
                ?.details?.imageDetails?.dockerImageRepo
                ?.let {
                    ImageRepository.fromRepoString(it)
                }
        }
}

data class ApplicationEdge(private val node: Application) : DefaultEdge<Application>(
    node,
    Cursor(node.name)
) {
    companion object {
        fun create(resource: ApplicationResource, applicationInstances: List<ApplicationInstance>) =
            ApplicationEdge(
                Application(
                    resource.name,
                    applicationInstances
                )
            )
    }
}

data class ApplicationsConnection(override val edges: List<ApplicationEdge>, override val pageInfo: PageInfo?) :
    Connection<ApplicationEdge>()

fun createApplicationEdge(
    resource: ApplicationResource,
    details: List<ApplicationInstanceDetailsResource>
): ApplicationEdge {
    val applicationInstances = createApplicationInstances(resource, details)
    return ApplicationEdge.create(resource, applicationInstances)
}