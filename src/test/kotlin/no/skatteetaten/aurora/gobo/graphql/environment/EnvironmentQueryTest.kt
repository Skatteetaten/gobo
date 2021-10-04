package no.skatteetaten.aurora.gobo.graphql.environment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentDeploymentRef
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentService
import no.skatteetaten.aurora.gobo.integration.boober.EnvironmentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.springboot.OpenShiftTokenIssuer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import org.springframework.security.test.context.support.WithMockUser

@Import(EnvironmentQuery::class, EnvironmentStatusDataLoader::class)
class EnvironmentQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getEnvironments.graphql")
    private lateinit var getEnvironmentsQuery: Resource

    @Value("classpath:graphql/queries/getEnvironmentsWithAutoDeploy.graphql")
    private lateinit var getEnvironmentsWithAutoDeployQuery: Resource

    @Value("classpath:graphql/queries/getEnvironmentNames.graphql")
    private lateinit var getEnvironmentNamesQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var environmentService: EnvironmentService

    @MockkBean
    private lateinit var openShiftTokenIssuer: OpenShiftTokenIssuer

    @BeforeEach
    fun setUp() {
        coEvery {
            environmentService.getEnvironments(any(), "utv")
        } returns createEnvironmentResources("utv", "gobo", "boober")

        coEvery {
            environmentService.getEnvironments(any(), "dev-utv")
        } returns createEnvironmentResources("dev-utv", "mokey")

        coEvery {
            applicationService.getApplicationDeployments(applicationDeploymentRefs = any())
        } returns createApplicationDeployments("utv", "gobo", "boober")
    }

    @WithMockUser(authorities = [clientGroup])
    @Test
    fun `Get environments`() {
        coEvery {
            openShiftTokenIssuer.getToken()
        } returns "token"

        webTestClient.queryGraphQL(getEnvironmentsQuery, mapOf("names" to listOf("utv", "dev-utv")), "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("environments[0].name").isEqualTo("utv")
            .graphqlDataWithPrefix("environments[0].affiliations[0]") {
                graphqlData("name").isEqualTo("aurora")
                graphqlData("applications.length()").isEqualTo(2)
                graphqlData("applications[0].name").isEqualTo("gobo")
                graphqlData("applications[0].status.state").isEqualTo(EnvironmentStatusType.COMPLETED.name)
            }
            .graphqlData("environments[1].name").isEqualTo("dev-utv")
            .graphqlDataWithPrefix("environments[1].affiliations[0]") {
                graphqlData("name").isEqualTo("aurora")
                graphqlData("applications.length()").isEqualTo(1)
                graphqlData("applications[0].name").isEqualTo("mokey")
                graphqlData("applications[0].status.state").isEqualTo(EnvironmentStatusType.INACTIVE.name)
            }
            .graphqlDoesNotContainErrors()

        coVerify(exactly = 1) { openShiftTokenIssuer.getToken() }
    }

    @WithMockUser(authorities = [adminGroup])
    @Test
    fun `Get environments with autoDeploy`() {
        webTestClient.queryGraphQL(
            getEnvironmentsWithAutoDeployQuery,
            mapOf("name" to "utv", "autoDeploy" to true),
            "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("environments[0].affiliations[0]") {
                graphqlData("name").isEqualTo("aurora")
                graphqlData("applications.length()").isEqualTo(2)
                graphqlData("applications[0].name").isEqualTo("gobo")
            }
            .graphqlDoesNotContainErrors()

        coVerify(exactly = 0) { openShiftTokenIssuer.getToken() }
    }

    @WithMockUser(authorities = [adminGroup])
    @Test
    fun `Get environments name`() {
        webTestClient.queryGraphQL(
            getEnvironmentNamesQuery,
            mapOf("name" to "utv"),
            "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("environments[0]") {
                graphqlData("name").isEqualTo("utv")
                graphqlData("affiliations[0].name").isEqualTo("aurora")
                graphqlData("affiliations[0].applications.length()").isEqualTo(2)
            }

        coVerify(exactly = 0) { openShiftTokenIssuer.getToken() }
    }

    private fun createEnvironmentResources(environment: String, vararg names: String) =
        listOf(
            EnvironmentResource(
                affiliation = "aurora",
                deploymentRefs = names.map {
                    EnvironmentDeploymentRef(
                        environment = environment,
                        application = it,
                        autoDeploy = true
                    )
                }
            )
        )

    private fun createApplicationDeployments(environment: String, vararg names: String) =
        names.map {
            ApplicationDeploymentResourceBuilder(
                affiliation = "aurora",
                environment = environment,
                name = it
            ).build()
        }
}
