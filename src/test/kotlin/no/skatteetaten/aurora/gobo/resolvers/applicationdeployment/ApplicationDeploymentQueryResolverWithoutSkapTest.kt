package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.ImageTagResourceBuilder
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import no.skatteetaten.aurora.gobo.integration.skap.RouteServiceReactive
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithoutDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Disabled("fix error handling")
class ApplicationDeploymentQueryResolverWithoutSkapTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeployment.graphql")
    private lateinit var getApplicationsQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var routeService: RouteService

    @MockkBean
    private lateinit var imageRegistryService: ImageRegistryServiceBlocking

    @MockkBean
    private lateinit var databaseServiceReactive: DatabaseServiceReactive

    @MockkBean
    private lateinit var routeServiceReactive: RouteServiceReactive

    @BeforeEach
    fun setUp() {
        coEvery { applicationService.getApplicationDeployment(any()) } returns ApplicationDeploymentResourceBuilder(
            id = "123",
            msg = "Hei"
        ).build()

        every {
            routeService.getSkapJobs(
                any(),
                any()
            )
        } throws IntegrationDisabledException("Skap integration is disabled for this environment")
        every { imageRegistryService.findTagsByName(any(), any()) } returns AuroraResponse(
            listOf(ImageTagResourceBuilder().build())
        )
    }

    @Test
    fun `Query for application deployment returns error message`() {
        val variables = mapOf("id" to "123")
        webTestClient.queryGraphQL(getApplicationsQuery, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("applicationDeployment") {
                graphqlData("id").isEqualTo("123")
                graphqlData("status.reports").exists()
                graphqlData("status.reasons").exists()
                graphqlData("message").exists()
                graphqlData("route.progressions").doesNotExist()
            }
            .graphqlErrorsFirst("message").isEqualTo("Skap integration is disabled for this environment")
    }
}
