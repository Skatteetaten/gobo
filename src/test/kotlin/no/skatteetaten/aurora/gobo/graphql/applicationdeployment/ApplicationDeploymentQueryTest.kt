package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.ImageTagResourceBuilder
import no.skatteetaten.aurora.gobo.SkapJobForBigipBuilder
import no.skatteetaten.aurora.gobo.SkapJobForWebsealBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageDataLoader
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.graphql.route.RouteDataLoader
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(ApplicationDeploymentQuery::class, ImageDataLoader::class, RouteDataLoader::class)
class ApplicationDeploymentQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeployment.graphql")
    private lateinit var getApplicationsQuery: Resource

    @Value("classpath:graphql/queries/getApplicationDeploymentsWithRef.graphql")
    private lateinit var getApplicationsWithRefQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var routeService: RouteService

    @MockkBean
    private lateinit var imageRegistryService: ImageRegistryService

    @BeforeEach
    fun setUp() {
        coEvery { applicationService.getApplicationDeployment(any<String>()) } returns ApplicationDeploymentResourceBuilder(
            id = "123",
            msg = "Hei"
        ).build()

        val websealjob = SkapJobForWebsealBuilder().build()
        val bigipJob = SkapJobForBigipBuilder().build()
        coEvery { routeService.getSkapJobs("namespace", "name-webseal") } returns listOf(websealjob)
        coEvery { routeService.getSkapJobs("namespace", "name-bigip") } returns listOf(bigipJob)
        coEvery { imageRegistryService.findTagsByName(any(), any()) } returns AuroraResponse(
            listOf(
                ImageTagResourceBuilder().build()
            )
        )
    }

    @AfterEach
    fun tearDown() {
        clearMocks(routeService)
    }

    @Test
    fun `Query for application deployment`() {
        val variables = mapOf("id" to "123")
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("applicationDeployment") {
                graphqlData("id").isEqualTo("123")
                graphqlData("status.reports").exists()
                graphqlData("status.reasons").exists()
                graphqlData("message").exists()
                graphqlData("route.websealJobs[0].id").isEqualTo("75745")
                graphqlData("route.websealJobs[0].host").isEqualTo("testing.test.no")
                graphqlData("route.bigipJobs[0].id").isEqualTo("465774")
                graphqlData("route.bigipJobs[0].asmPolicy").isEqualTo("testing-get")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for application deployment partial result`() {
        coEvery { routeService.getSkapJobs("namespace", "partial-result-webseal") } returns listOf(
            SkapJobForWebsealBuilder(name = "partial-result").build()
        )
        coEvery {
            routeService.getSkapJobs(
                "namespace",
                "name-bigip"
            )
        } throws IntegrationDisabledException("Skap integration is disabled for this environment")

        val variables = mapOf("id" to "123")
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("applicationDeployment") {
                graphqlData("id").isEqualTo("123")
                graphqlData("status.reports").exists()
                graphqlData("status.reasons").exists()
                graphqlData("message").exists()
            }
            .graphqlErrorsFirst("message").isNotEmpty
    }

    @Test
    fun `Query for application deployment with ApplicationDeploymentRef`() {
        coEvery { applicationService.getApplicationDeployments(any()) } returns listOf(
            ApplicationDeploymentResourceBuilder().build()
        )

        val variables = mapOf(
            "input" to mapOf("environment" to "environment", "application" to "name")
        )
        webTestClient.queryGraphQL(getApplicationsWithRefQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("applicationDeployments[0].id").isEqualTo("id")
            .graphqlDoesNotContainErrors()
    }
}
