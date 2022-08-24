package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.PROFILE_WITH_DBH_AND_SKAP
import no.skatteetaten.aurora.gobo.graphql.application.ApplicationQuery
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentQuery
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.BooberWebClient
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.web.reactive.function.client.WebClient
import uk.q3c.rest.hal.HalLink
import uk.q3c.rest.hal.Links

@Profile(PROFILE_WITH_DBH_AND_SKAP)
@TestConfiguration
class DeploymentSpecTestConfig(val server: MockWebServer = MockWebServer()) {
    @Bean
    fun booberWebClient() = BooberWebClient("", WebClient.create(server.url), jacksonObjectMapper())
}

@Import(
    DeploymentSpecTestConfig::class,
    ApplicationQuery::class,
    ApplicationDeploymentQuery::class,
    DeploymentSpecDataLoader::class,
    ApplicationDeploymentDetailsDataLoader::class
)
class DeploymentSpecTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeploymentWithDeploymentSpec.graphql")
    private lateinit var getApplicationsWithDeploymentSpec: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @Autowired
    private lateinit var testConfig: DeploymentSpecTestConfig

    @Test
    fun `Query for application deployment with deployment spec`() {
        coEvery {
            applicationService.getApplicationDeploymentDetails(any(), any())
        } returns ApplicationDeploymentDetailsBuilder(
            resourceLinks = Links().apply {
                add("DeploymentSpecCurrent", HalLink(testConfig.server.url))
                add("DeploymentSpecDeployed", HalLink(testConfig.server.url))
            }
        ).build()

        coEvery { applicationService.getApplicationDeployment(any()) } returns ApplicationDeploymentResourceBuilder().build()

        testConfig.server.execute(Response("""{ "test1":"abc" }""")) {
            webTestClient.queryGraphQL(getApplicationsWithDeploymentSpec, mapOf("id" to "abc123"), "test-token")
                .expectStatus().isOk
                .expectBody()
                .graphqlData("applicationDeployment.details.deploymentSpecs.current.jsonRepresentation").isNotEmpty
                .graphqlDoesNotContainErrors()
        }
    }
}
