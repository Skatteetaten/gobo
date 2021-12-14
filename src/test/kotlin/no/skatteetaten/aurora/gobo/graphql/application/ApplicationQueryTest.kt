package no.skatteetaten.aurora.gobo.graphql.application

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails.ApplicationDeploymentDetailsDataLoader
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.permission.PermissionDataLoader
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.AuroraNamespacePermissions
import no.skatteetaten.aurora.gobo.integration.mokey.PermissionService
import no.skatteetaten.aurora.gobo.service.AffiliationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(
    AffiliationQuery::class,
    ApplicationQuery::class,
    PermissionDataLoader::class,
    ApplicationDeploymentDetailsDataLoader::class,
    ApplicationDataLoader::class
)
class ApplicationQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplications.graphql")
    private lateinit var getApplicationsQuery: Resource

    @Value("classpath:graphql/queries/getApplicationsForAffiliation.graphql")
    private lateinit var getApplicationsForAffiliationQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var permissionService: PermissionService

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    private val affiliations = listOf("paas", "aurora")

    @BeforeEach
    fun setUp() {
        coEvery { applicationService.getApplications(affiliations) } returns listOf(
            ApplicationResourceBuilder(affiliation = "paas").build(),
            ApplicationResourceBuilder(affiliation = "aurora").build()
        )
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
    }

    @Test
    fun `Query for applications given affiliations`() {
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

    @Test
    fun `Query for applications for affiliation`() {
        val variables = mapOf("affiliations" to affiliations)
        webTestClient.queryGraphQL(getApplicationsForAffiliationQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.totalCount").isEqualTo(2)
            .graphqlDataWithPrefix("affiliations.edges[0].node") {
                graphqlData("applications[0].name").isEqualTo("name")
            }
            .graphqlDoesNotContainErrors()
    }
}
