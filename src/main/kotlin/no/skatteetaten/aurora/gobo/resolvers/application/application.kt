package no.skatteetaten.aurora.gobo.resolvers.application

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.application.ApplicationInstanceDetailsResource
import no.skatteetaten.aurora.gobo.application.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.ApplicationInstance
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.Status
import no.skatteetaten.aurora.gobo.resolvers.applicationinstance.Version
import no.skatteetaten.aurora.gobo.resolvers.applicationinstancedetails.ApplicationInstanceDetails
import no.skatteetaten.aurora.gobo.resolvers.applicationinstancedetails.GitInfo
import no.skatteetaten.aurora.gobo.resolvers.applicationinstancedetails.ImageDetails
import no.skatteetaten.aurora.gobo.resolvers.applicationinstancedetails.PodResource
import org.springframework.hateoas.Link

data class Application(
    val name: String,
    val tags: List<String>,
    val applicationInstances: List<ApplicationInstance>
)

data class ApplicationEdge(private val node: Application) : DefaultEdge<Application>(
    node,
    Cursor(node.name)
)

data class ApplicationsConnection(override val edges: List<ApplicationEdge>, override val pageInfo: PageInfo?) :
    Connection<ApplicationEdge>()

fun createApplicationEdge(
    resource: ApplicationResource,
    details: List<ApplicationInstanceDetailsResource>
): ApplicationEdge {
    val applicationInstances = resource.applicationInstances.map { instance ->
        val detailsResource =
            details.find { it.getLink(Link.REL_SELF).href == instance.getLink("ApplicationInstanceDetails").href }

        val applicationInstanceDetails = detailsResource?.let {
            ApplicationInstanceDetails(
                buildTime = detailsResource.buildTime,
                imageDetails = it.imageDetails.let {
                    ImageDetails(it.imageBuildTime, it.dockerImageReference)
                },
                gitInfo = it.gitInfo.let {
                    GitInfo(it.commitId, it.commitTime)
                },
                podResources = it.podResources.map {
                    PodResource(
                        it.name,
                        it.status,
                        it.restartCount,
                        it.ready,
                        it.startTime,
                        it.getLink("metrics").href
                    )
                }
            )
        }

        ApplicationInstance(
            instance.affiliation,
            instance.environment,
            instance.namespace,
            Status(instance.status.code, instance.status.comment),
            Version(
                instance.version.deployTag,
                instance.version.auroraVersion
            ),
            applicationInstanceDetails
        )
    }

    return ApplicationEdge(
        Application(
            resource.name,
            resource.tags,
            applicationInstances
        )
    )
}