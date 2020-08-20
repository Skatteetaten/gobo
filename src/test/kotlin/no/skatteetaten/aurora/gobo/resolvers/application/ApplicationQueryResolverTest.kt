package no.skatteetaten.aurora.gobo.resolvers.application

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraNamespacePermissions
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import reactor.kotlin.core.publisher.toMono

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
        every { applicationService.getApplications(affiliations) } returns listOf(ApplicationResourceBuilder().build()).toMono()
        every { permissionService.getPermission(any(), any()) } returns AuroraNamespacePermissions(
            view = true,
            admin = true,
            namespace = "namespace"
        ).toMono()
        every {
            applicationService.getApplicationDeploymentDetails(
                any(),
                any()
            )
        } returns ApplicationDeploymentDetailsBuilder().build().toMono()

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
