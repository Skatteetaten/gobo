package no.skatteetaten.aurora.gobo.graphql.application

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraNamespacePermissions
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class ApplicationQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplications.graphql")
    private lateinit var getApplicationsQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var permissionService: PermissionService

    @Test
    fun `Query for applications given affiliations`() {
        val affiliations = listOf("paas")
        coEvery { applicationService.getApplications(affiliations) } returns listOf(ApplicationResourceBuilder().build())
        coEvery { permissionService.getPermission(any(), any()) } returns AuroraNamespacePermissions(
            view = true,
            admin = true,
            namespace = "namespace"
        )
        coEvery {
            applicationService.getApplicationDeploymentDetails(
                any(),
                any()
            )
        } returns ApplicationDeploymentDetailsBuilder().build()

        val variables = mapOf("affiliations" to affiliations)
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("applications.totalCount").isNumber
            .graphqlDataWithPrefix("applications.edges[0].node") {
                graphqlData("applicationDeployments[0].affiliation.name").isNotEmpty
                graphqlData("applicationDeployments[0].namespace.name").isNotEmpty
                graphqlData("applicationDeployments[0].namespace.permission.paas.admin").isNotEmpty
                graphqlData("applicationDeployments[0].details.updatedBy").isNotEmpty
                graphqlData("applicationDeployments[0].details.buildTime").isNotEmpty
                graphqlData("applicationDeployments[0].details.deployDetails.paused").isEqualTo(false)
                graphqlData("imageRepository.repository").doesNotExist()
            }
            .graphqlDoesNotContainErrors()
    }
}