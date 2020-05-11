package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.ImageTagResourceBuilder
import no.skatteetaten.aurora.gobo.SkapJobBuilder
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class ApplicationDeploymentQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeployment.graphql")
    private lateinit var getApplicationsQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockkBean
    private lateinit var routeService: RouteService

    @MockkBean
    private lateinit var imageRegistryService: ImageRegistryServiceBlocking

    @BeforeEach
    fun setUp() {
        every { applicationService.getApplicationDeployment(any()) } returns ApplicationDeploymentResourceBuilder(
            id = "123",
            msg = "Hei"
        ).build()

        every { routeService.getSkapJobs(any(), any()) } returns listOf(SkapJobBuilder().build())
        every { routeService.getSkapJobs("namespace", "name-webseal") } returns listOf(SkapJobBuilder().build())
        every { imageRegistryService.findTagsByName(any(), any()) } returns AuroraResponse(
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
                graphqlData("route.progressions[0].id").isEqualTo("54523")
            }
            .graphqlDoesNotContainErrors()
    }
}
