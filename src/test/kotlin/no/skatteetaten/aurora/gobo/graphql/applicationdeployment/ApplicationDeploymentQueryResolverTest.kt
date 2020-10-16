package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.ImageTagResourceBuilder
import no.skatteetaten.aurora.gobo.SkapJobForBigipBuilder
import no.skatteetaten.aurora.gobo.SkapJobForWebsealBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class ApplicationDeploymentQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeployment.graphql")
    private lateinit var getApplicationsQuery: Resource

    @Value("classpath:graphql/queries/getApplicationDeploymentsWithRef.graphql")
    private lateinit var getApplicationsWithRefQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var routeService: RouteService

    @MockkBean
    private lateinit var imageRegistryService: ImageRegistryServiceBlocking

    @BeforeEach
    fun setUp() {
        coEvery { applicationService.getApplicationDeployment(any<String>()) } returns ApplicationDeploymentResourceBuilder(
            id = "123",
            msg = "Hei"
        ).build()

        val websealjob = SkapJobForWebsealBuilder().build()
        val bigipJob = SkapJobForBigipBuilder().build()
        every { routeService.getSkapJobs("namespace", "name-webseal") } returns listOf(websealjob)
        every { routeService.getSkapJobs("namespace", "name-bigip") } returns listOf(bigipJob)
        coEvery { imageRegistryService.findTagsByName(any(), any()) } returns AuroraResponse(
            listOf(
                ImageTagResourceBuilder().build()
            )
        )
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
    fun `Query for application deployment with ApplicationDeploymentRef`() {
        coEvery { applicationService.getApplicationDeployment(any<List<ApplicationDeploymentRef>>()) } returns listOf(
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