package no.skatteetaten.aurora.gobo.graphql.environment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(EnvironmentQuery::class)
class EnvironmentQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getEnvironments.graphql")
    private lateinit var getEnvironmentsQuery: Resource

    @Value("classpath:graphql/queries/getEnvironmentsWithAutoDeploy.graphql")
    private lateinit var getEnvironmentsWithAutoDeployQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var environmentService: EnvironmentService

    @BeforeEach
    fun setUp() {
        coEvery {
            environmentService.getEnvironments(any(), any())
        } returns listOf(
            EnvironmentResource(
                "aurora",
                listOf(
                    EnvironmentDeploymentRef("utv", "gobo", true),
                    EnvironmentDeploymentRef("utv", "boober", false)
                )
            )
        )

        coEvery { applicationService.getApplicationDeployments(applicationDeploymentRefs = any()) } returns listOf(
            ApplicationDeploymentResourceBuilder(affiliation = "aurora", environment = "utv", name = "gobo").build(),
            ApplicationDeploymentResourceBuilder(affiliation = "aurora", environment = "utv", name = "boober").build()
        )
    }

    @Test
    fun `Get environments`() {
        webTestClient.queryGraphQL(getEnvironmentsQuery, mapOf("name" to "utv"), "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("environments[0].name").isEqualTo("utv")
            .graphqlDataWithPrefix("environments[0].affiliations[0]") {
                graphqlData("name").isEqualTo("aurora")
                graphqlData("applications.length()").isEqualTo(2)
                graphqlData("applications[0].name").isEqualTo("gobo")
                graphqlData("applications[0].status.state").isEqualTo(EnvironmentStatusType.COMPLETED.name)
            }
            .graphqlDoesNotContainErrors()
    }

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
                graphqlData("applications.length()").isEqualTo(1)
                graphqlData("applications[0].name").isEqualTo("gobo")
            }
            .graphqlDoesNotContainErrors()
    }
}
