package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.ImageTagResourceBuilder
import no.skatteetaten.aurora.gobo.SkapJobForBigipBuilder
import no.skatteetaten.aurora.gobo.SkapJobForWebsealBuilder
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.skap.RouteServiceReactive
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Disabled("unstable test")
class ApplicationDeploymentQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeployment.graphql")
    private lateinit var getApplicationsQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var routeService: RouteServiceReactive

    @MockkBean
    private lateinit var imageRegistryService: ImageRegistryServiceBlocking

    @BeforeEach
    fun setUp() {
        coEvery { applicationService.getApplicationDeployment(any()) } returns
            ApplicationDeploymentResourceBuilder(
                id = "123",
                msg = "Hei"
            ).build()

        val websealjob = SkapJobForWebsealBuilder().build()
        val bigipJob = SkapJobForBigipBuilder().build()
        coEvery { routeService.getSkapJobs("namespace", "name-webseal") } returns listOf(websealjob)
        coEvery { routeService.getSkapJobs("namespace", "name-bigip") } returns listOf(bigipJob)

        every { imageRegistryService.findTagsByName(any(), any()) } returns
            AuroraResponse(
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
}
