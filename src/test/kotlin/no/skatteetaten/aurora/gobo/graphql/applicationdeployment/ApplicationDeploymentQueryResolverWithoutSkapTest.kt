package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.ImageTagResourceBuilder
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithoutDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class ApplicationDeploymentQueryResolverWithoutSkapTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeployment.graphql")
    private lateinit var getApplicationsQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var imageRegistryService: ImageRegistryServiceBlocking

    @MockkBean
    private lateinit var databaseServiceReactive: DatabaseServiceReactive

    @MockkBean
    private lateinit var routeService: RouteService

    @BeforeEach
    fun setUp() {
        coEvery { applicationService.getApplicationDeployment(any<String>()) } returns ApplicationDeploymentResourceBuilder(
            id = "123",
            msg = "Hei"
        ).build()

        coEvery {
            routeService.getSkapJobs(
                any(),
                any()
            )
        } throws IntegrationDisabledException("Skap integration is disabled for this environment")
        coEvery { imageRegistryService.findTagsByName(any(), any()) } returns AuroraResponse(
            listOf(ImageTagResourceBuilder().build())
        )
    }

    @Test
    fun `Query for application deployment returns error message`() {
        val variables = mapOf("id" to "123")
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            // TODO fix partial result
            /*
            .graphqlDataWithPrefix("applicationDeployment") {
                graphqlData("id").isEqualTo("123")
                graphqlData("status.reports").exists()
                graphqlData("status.reasons").exists()
                graphqlData("message").exists()
                graphqlData("route.progressions").doesNotExist()
            }
             */
            .graphqlErrorsFirst("message")
            .isEqualTo("Exception while fetching data (/applicationDeployment/route) : Skap integration is disabled for this environment")
    }
}
