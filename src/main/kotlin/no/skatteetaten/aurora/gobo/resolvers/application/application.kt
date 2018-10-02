package no.skatteetaten.aurora.gobo.resolvers.application

import graphql.relay.DefaultEdge
import graphql.relay.PageInfo
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.Connection
import no.skatteetaten.aurora.gobo.resolvers.Cursor
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeploymentBuilder
import no.skatteetaten.aurora.gobo.resolvers.createPageInfo

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
                    resource.identifier,
                    resource.name,
                    applicationDeployments
                )
            )
    }
}

data class ApplicationsConnection(
    override val edges: List<ApplicationEdge>,
    override val pageInfo: PageInfo = createPageInfo(edges)
) : Connection<ApplicationEdge>()

private val deploymentBuilder = ApplicationDeploymentBuilder()

fun createApplicationEdges(applicationResources: List<ApplicationResource>): List<ApplicationEdge> =
    applicationResources.map { createApplicationEdge(it) }

fun createApplicationEdge(it: ApplicationResource) =
    ApplicationEdge.create(it, deploymentBuilder.createApplicationDeployments(it))