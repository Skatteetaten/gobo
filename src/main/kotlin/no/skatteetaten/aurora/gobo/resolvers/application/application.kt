package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.GoboEdge
import no.skatteetaten.aurora.gobo.resolvers.GoboPageInfo
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeploymentBuilder
import no.skatteetaten.aurora.gobo.resolvers.createPageInfo
import java.time.Instant

data class Certificate(
    val id: String,
    val dn: String,
    val issuedDate: Instant?,
    val revokedDate: Instant?,
    val expiresDate: Instant?
)

data class Application(
    val id: String,
    val name: String,
    val applicationDeployments: List<ApplicationDeployment>
)

data class ApplicationEdge(val node: Application) : GoboEdge(node.name) {
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
    val edges: List<ApplicationEdge>,
    val pageInfo: GoboPageInfo = createPageInfo(edges)
)

private val deploymentBuilder = ApplicationDeploymentBuilder()

fun createApplicationEdges(applicationResources: List<ApplicationResource>): List<ApplicationEdge> =
    applicationResources.map { createApplicationEdge(it) }

fun createApplicationEdge(it: ApplicationResource) =
    ApplicationEdge.create(it, deploymentBuilder.createApplicationDeployments(it))
