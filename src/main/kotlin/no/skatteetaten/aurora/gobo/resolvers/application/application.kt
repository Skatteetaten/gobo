package no.skatteetaten.aurora.gobo.resolvers.application

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeploymentBuilder
import no.skatteetaten.aurora.gobo.resolvers.createPageInfo
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentDetailsResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource

data class Application(
    val id: String,
    val name: String,
    val applicationDeployments: List<ApplicationDeployment>
)

data class ApplicationEdge(private val node: Application) : DefaultEdge<Application>(
    node,
    Cursor(node.name)
) {
    companion object {
        fun create(resource: ApplicationResource, applicationDeployments: List<ApplicationDeployment>) =
            ApplicationEdge(
                Application(
                    resource.identifier!!,
                    resource.name!!,
                    applicationDeployments
                )
            )
    }
}

data class ApplicationsConnection(
    override val edges: List<ApplicationEdge>,
    override val pageInfo: PageInfo = createPageInfo(edges)
) : Connection<ApplicationEdge>()

fun createApplicationEdges(
    applicationResources: List<ApplicationResource>,
    detailResources: List<ApplicationDeploymentDetailsResource>
): List<ApplicationEdge> {
    val deploymentBuilder = ApplicationDeploymentBuilder(detailResources)
    return applicationResources.map { ApplicationEdge.create(it, deploymentBuilder.createApplicationDeployments(it)) }
}