package no.skatteetaten.aurora.gobo.graphql.environment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.DeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.phil.PhilService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(DeployEnvironmentMutation::class)
class DeployEnvironmentMutationTest : GraphQLTestWithDbhAndSkap() {
    @Value("classpath:graphql/mutations/deployEnvironment.graphql")
    private lateinit var deployEnvironmentMutation: Resource

    @MockkBean
    private lateinit var philService: PhilService

    @Test
    fun `Deploy environment`() {
        coEvery { philService.deployEnvironment(any(), any()) } returns listOf(DeploymentResourceBuilder().build())

        webTestClient.queryGraphQL(deployEnvironmentMutation, DeploymentEnvironmentInput("dev-utv"), "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("deployEnvironment.[0]") {
                graphqlData("deployId").isEqualTo("123")
                graphqlData("deploymentRef.cluster").isEqualTo("utv")
                graphqlData("deploymentRef.affiliation").isEqualTo("aurora")
                graphqlData("deploymentRef.environment").isEqualTo("dev-utv")
                graphqlData("deploymentRef.application").isEqualTo("gobo")
                graphqlData("timestamp").isNotEmpty
                graphqlData("message").isEmpty
            }
            .graphqlDoesNotContainErrors()
    }
}
