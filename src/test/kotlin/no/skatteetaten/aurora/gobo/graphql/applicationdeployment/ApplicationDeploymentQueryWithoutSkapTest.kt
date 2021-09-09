package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.ImageTagResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithoutDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.graphql.imagerepository.ImageDataLoader
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.graphql.route.RouteDataLoader
import no.skatteetaten.aurora.gobo.integration.cantus.AuroraResponse
import no.skatteetaten.aurora.gobo.integration.cantus.ImageRegistryService
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.graphql.auroraconfig.AuroraConfigFileResourceDataLoader
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService

@Import(
    ApplicationDeploymentQuery::class,
    ImageDataLoader::class,
    RouteDataLoader::class,
    AuroraConfigFileResourceDataLoader::class
)
class ApplicationDeploymentQueryWithoutSkapTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeployment.graphql")
    private lateinit var getApplicationsQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var imageRegistryService: ImageRegistryService

    @MockkBean
    private lateinit var databaseServiceReactive: DatabaseServiceReactive

    @MockkBean
    private lateinit var auroraConfigService: AuroraConfigService

    @MockkBean
    private lateinit var routeService: RouteService

    @BeforeEach
    fun setUp() {
        coEvery { applicationService.getApplicationDeployment(any<String>()) } returns ApplicationDeploymentResourceBuilder(
            id = "123",
            msg = "Hei"
        ).build()

        coEvery {
            applicationService.getApplicationDeploymentDetails(
                any(),
                any()
            )
        } returns ApplicationDeploymentDetailsBuilder().build()

        coEvery { auroraConfigService.getAuroraConfigFiles(any(), any(), any(), any()) } returns emptyList()

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
            .graphqlDataWithPrefix("applicationDeployment") {
                graphqlData("id").isEqualTo("123")
                graphqlData("status.reports").exists()
                graphqlData("status.reasons").exists()
                graphqlData("message").exists()
                graphqlData("route.progressions").doesNotExist()
                graphqlData("files").isEmpty
            }
            .graphqlErrorsFirst("message")
            .isEqualTo("Skap integration is disabled for this environment")
    }
}
