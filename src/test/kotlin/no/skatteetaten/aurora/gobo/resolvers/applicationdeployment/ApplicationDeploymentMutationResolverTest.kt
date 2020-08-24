package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.isTrue
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class ApplicationDeploymentMutationResolverTest : GraphQLTestWithDbhAndSkap() {
    @Value("classpath:graphql/mutations/redeployWithVersion.graphql")
    private lateinit var redeployWithVersionMutation: Resource

    @Value("classpath:graphql/mutations/redeployWithCurrentVersion.graphql")
    private lateinit var redeployWithCurrentVersionMutation: Resource

    @Value("classpath:graphql/mutations/refreshApplicationDeployment.graphql")
    private lateinit var refreshApplicationDeploymentByDeploymentIdMutation: Resource

    @Value("classpath:graphql/mutations/refreshApplicationDeployments.graphql")
    private lateinit var refreshApplicationDeploymentsByAffiliationsMutation: Resource

    @Value("classpath:graphql/mutations/deleteApplicationDeployment.graphql")
    private lateinit var deleteApplicationDeploymentMutation: Resource

    @MockkBean(relaxed = true)
    private lateinit var applicationUpgradeService: ApplicationUpgradeService

    @MockkBean
    private lateinit var applicationDeploymentService: ApplicationDeploymentService

    @BeforeEach
    fun setUp() {
        every { applicationUpgradeService.refreshApplicationDeployment(any(), any()) } returns true
        every { applicationDeploymentService.deleteApplicationDeployment(any(), any()) } returns true
    }

    @Test
    fun `Mutate application deployment version`() {
        val variables = mapOf(
            "input" to mapOf(
                "applicationDeploymentId" to "123",
                "version" to "1"
            )
        )
        webTestClient.queryGraphQL(redeployWithVersionMutation, variables, "test-token").expectBody()
            .graphqlData("redeployWithVersion")
            .isNotEmpty
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Mutate application deployment current version`() {
        val variables = mapOf(
            "input" to mapOf(
                "applicationDeploymentId" to "123"
            )
        )
        webTestClient.queryGraphQL(redeployWithCurrentVersionMutation, variables, "test-token").expectBody()
            .graphqlData("redeployWithCurrentVersion").isNotEmpty
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Mutate refresh application deployment by applicationDeploymentId`() {
        val variables = mapOf(
            "input" to mapOf(
                "applicationDeploymentId" to "123"
            )
        )
        webTestClient.queryGraphQL(refreshApplicationDeploymentByDeploymentIdMutation, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("refreshApplicationDeployment").isNotEmpty
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Mutate refresh application deployment by affiliations`() {
        val variables = mapOf(
            "input" to mapOf(
                "affiliations" to listOf("aurora")
            )
        )
        webTestClient.queryGraphQL(refreshApplicationDeploymentsByAffiliationsMutation, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("refreshApplicationDeployments").isNotEmpty
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Delete application deployment`() {

        val variables = mapOf(
            "input" to mapOf(
                "namespace" to "aurora-dev",
                "name" to "konsoll"
            )
        )

        webTestClient.queryGraphQL(deleteApplicationDeploymentMutation, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("deleteApplicationDeployment").isTrue()
            .graphqlDoesNotContainErrors()
    }
}
