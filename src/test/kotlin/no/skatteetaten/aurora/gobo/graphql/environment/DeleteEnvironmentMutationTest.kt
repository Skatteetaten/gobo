package no.skatteetaten.aurora.gobo.graphql.environment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.DeletionResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.isTrue
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.phil.EnvironmentService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(DeleteEnvironmentMutation::class)
class DeleteEnvironmentMutationTest : GraphQLTestWithDbhAndSkap() {
    @Value("classpath:graphql/mutations/deleteEnvironment.graphql")
    private lateinit var deleteEnvironmentMutation: Resource

    @MockkBean
    private lateinit var environmentService: EnvironmentService

    @Test
    fun `Deploy environment`() {
        coEvery { environmentService.deleteEnvironment(any(), any()) } returns listOf(DeletionResourceBuilder().build())

        webTestClient.queryGraphQL(deleteEnvironmentMutation, DeleteEnvironmentInput("dev-utv"), "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("deleteEnvironment.[0]") {
                graphqlData("deploymentRef.cluster").isEqualTo("utv")
                graphqlData("deploymentRef.affiliation").isEqualTo("aurora")
                graphqlData("deploymentRef.environment").isEqualTo("dev-utv")
                graphqlData("deploymentRef.application").isEqualTo("gobo")
                graphqlData("timestamp").isNotEmpty
                graphqlData("message").isEmpty
                graphqlData("deleted").isTrue()
            }
            .graphqlDoesNotContainErrors()
    }
}
