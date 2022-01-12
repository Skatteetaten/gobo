package no.skatteetaten.aurora.gobo.graphql.application

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import no.skatteetaten.aurora.gobo.graphql.GoboEdge
import no.skatteetaten.aurora.gobo.graphql.GoboPageInfo
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeployment
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentBuilder
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentRef
import no.skatteetaten.aurora.gobo.graphql.createPageInfo
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageRepository
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationResource
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
    @GraphQLIgnore
    val applicationDeployments: List<ApplicationDeployment>
) {
    fun applicationDeployments(applicationDeploymentRefs: List<ApplicationDeploymentRef>? = null) =
        applicationDeploymentRefs?.mapNotNull { ref ->
            applicationDeployments.find { it.environment == ref.environment && it.name == ref.application }
        } ?: applicationDeployments

    fun imageRepository() =
        this.applicationDeployments.asSequence()
            .mapNotNull { it.dockerImageRepo }
            .map { ImageRepository.fromRepoString(it) }
            .firstOrNull { it.registryUrl != null && !DockerRegistryUtil.isInternal(it.registryUrl) }
}

data class ApplicationEdge(
    val node: Application
) : GoboEdge(node.name) {
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
    val pageInfo: GoboPageInfo = createPageInfo(edges),
    val totalCount: Int = edges.size
)

private val deploymentBuilder = ApplicationDeploymentBuilder()

fun createApplicationEdges(applicationResources: List<ApplicationResource>): List<ApplicationEdge> =
    applicationResources.map { createApplicationEdge(it) }

fun createApplicationEdge(it: ApplicationResource) =
    ApplicationEdge.create(it, deploymentBuilder.createApplicationDeployments(it))

// FIXME docker registry, not to happy with this :(
object DockerRegistryUtil {
    private val ipV4WithPortRegex =
        "^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]):([0-9]{1,4})(.*)\$".toRegex()

    fun isInternal(registry: String) =
        registry == "docker-registry.default.svc:5000" || registry.matches(ipV4WithPortRegex)
}
