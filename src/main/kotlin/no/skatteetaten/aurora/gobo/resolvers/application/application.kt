package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
import no.skatteetaten.aurora.gobo.resolvers.GoboEdge
import no.skatteetaten.aurora.gobo.resolvers.GoboPageInfo
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.resolvers.applicationdeployment.ApplicationDeploymentBuilder
import no.skatteetaten.aurora.gobo.resolvers.createPageInfo
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageRepository
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
) {
    fun imageRepository() =
        this.applicationDeployments.asSequence()
            .mapNotNull { it.dockerImageRepo }
            .map { ImageRepository.fromRepoString(it) }
            .firstOrNull { it.registryUrl != null && !isInternal(it.registryUrl) }

    // FIXME move the isInternal code somewhere else?
    private val ipV4WithPortRegex =
        "^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]):([0-9]{1,4})(.*)\$".toRegex()

    fun isInternal(registry: String) =
        registry == "docker-registry.default.svc:5000" || registry.matches(ipV4WithPortRegex)
}

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
    val totalCount: Int = edges.size,
    val pageInfo: GoboPageInfo = createPageInfo(edges)
)

private val deploymentBuilder = ApplicationDeploymentBuilder()

fun createApplicationEdges(applicationResources: List<ApplicationResource>): List<ApplicationEdge> =
    applicationResources.map { createApplicationEdge(it) }

fun createApplicationEdge(it: ApplicationResource) =
    ApplicationEdge.create(it, deploymentBuilder.createApplicationDeployments(it))
