package no.skatteetaten.aurora.gobo.graphql.application

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails.ApplicationDeploymentDetailsBatchDataLoader
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageRepoDto
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageTag
import no.skatteetaten.aurora.gobo.graphql.imagerepository.IsLatestDigestBatchDataLoader
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.cantus.ImageTagDto
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraNamespacePermissions
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

// TODO should return DeploymentSpecCurrent in ApplicationDeploymentDetailsBuilder
@Import(ApplicationQuery::class, ApplicationDeploymentDetailsBatchDataLoader::class, IsLatestDigestBatchDataLoader::class)
class ApplicationWithLatestDigestQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationsWithLatestDigest.graphql")
    private lateinit var getApplicationsQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var imageRegistryService: ImageRegistryService

    @MockkBean
    private lateinit var permissionService: PermissionService

    @Test
    fun `Query for latest image from repo`() {
        val affiliations = listOf("paas")

        val details = ApplicationDeploymentDetailsBuilder().build()

        val tag = ImageTag.fromTagString(details.imageDetails!!.dockerImageTagReference!!)
        val imageRepoDto = tag.imageRepository.toImageRepo()

        coEvery {
            imageRegistryService.findImageTagDto(
                imageRepoDto,
                tag.name,
                "test-token"
            )
        } returns ImageTagDto(
            imageTag = "abc",
            imageRepoDto = ImageRepoDto(null, "aurora", "gobo", null),
            dockerDigest = "sha256:123"
        )

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
            .graphqlData("applications.totalCount").isNumber
            .graphqlDataWithPrefix("applications.edges[0].node.applicationDeployments[0].details.imageDetails") {
                graphqlData("dockerImageTagReference").isEqualTo("docker.registry/group/name:2")
                graphqlData("digest").isEqualTo("sha256:123")
                graphqlData("isLatestDigest").isEqualTo(true)
            }
            .graphqlDoesNotContainErrors()
    }
}
