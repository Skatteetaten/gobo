package no.skatteetaten.aurora.gobo.resolvers.application

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraNamespacePermissions
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.resolvers.printResult
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Disabled
class ApplicationWithLatestDigestQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationsWithLatestDigest.graphql")
    private lateinit var getApplicationsQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var imageRegistryServiceBlocking: ImageRegistryServiceBlocking

    @MockkBean
    private lateinit var permissionService: PermissionService

    @Disabled("Implement image resolvers")
    @Test
    fun `Query for latest image from repo`() {
        val affiliations = listOf("paas")

        val details = ApplicationDeploymentDetailsBuilder().build()

        val tag = ImageTag.fromTagString(details.imageDetails!!.dockerImageTagReference!!)
        val imageRepoDto = tag.imageRepository.toImageRepo()

        coEvery {
            imageRegistryServiceBlocking.resolveTagToSha(
                imageRepoDto,
                tag.name,
                "test-token"
            )
        } returns "sha256:123"

        coEvery { applicationService.getApplications(affiliations) } returns listOf(ApplicationResourceBuilder().build())

        coEvery { permissionService.getPermission(any(), any()) } returns AuroraNamespacePermissions(
            view = true,
            admin = true,
            namespace = "namespace"
        )

        coEvery { applicationService.getApplicationDeploymentDetails(any(), any()) } returns details

        val variables = mapOf("affiliations" to affiliations)
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .printResult()
            /*
            .graphqlData("applications.totalCount").isNumber
            .graphqlDataWithPrefix("applications.edges[0].node.applicationDeployments[0].details.imageDetails") {
                graphqlData("dockerImageTagReference").isEqualTo("docker.registry/group/name:2")
                graphqlData("digest").isEqualTo("sha256:123")
                graphqlData("isLatestDigest").isEqualTo(true)
            }
            .graphqlDoesNotContainErrors()
             */
    }
}
