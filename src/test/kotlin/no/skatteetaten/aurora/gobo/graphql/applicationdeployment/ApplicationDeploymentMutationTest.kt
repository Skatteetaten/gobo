package no.skatteetaten.aurora.gobo.graphql.applicationdeployment

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.isTrue
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.boober.BooberApplicationRef
import no.skatteetaten.aurora.gobo.integration.boober.BooberDeleteResponse
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationDeploymentResource
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.StatusResource
import no.skatteetaten.aurora.gobo.integration.mokey.VersionResource
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import java.time.Instant

@Import(ApplicationDeploymentMutation::class)
class ApplicationDeploymentMutationTest : GraphQLTestWithDbhAndSkap() {
    @Value("classpath:graphql/mutations/redeployWithCurrentVersion.graphql")
    private lateinit var redeployWithCurrentVersionMutation: Resource

    @Value("classpath:graphql/mutations/refreshApplicationDeployment.graphql")
    private lateinit var refreshApplicationDeploymentByDeploymentIdMutation: Resource

    @Value("classpath:graphql/mutations/refreshApplicationDeployments.graphql")
    private lateinit var refreshApplicationDeploymentsByAffiliationsMutation: Resource

    @Value("classpath:graphql/mutations/deleteApplicationDeployment.graphql")
    private lateinit var deleteApplicationDeploymentMutation: Resource

    @Value("classpath:graphql/mutations/deleteApplicationDeployments.graphql")
    private lateinit var deleteApplicationDeploymentsMutation: Resource

    @MockkBean(relaxed = true)
    private lateinit var applicationUpgradeService: ApplicationUpgradeService

    @MockkBean(relaxed = true)
    private lateinit var applicationDeploymentService: ApplicationDeploymentService

    @MockkBean(relaxed = true)
    private lateinit var applicationService: ApplicationService

    @BeforeEach
    fun setUp() {
        coEvery { applicationUpgradeService.deployCurrentVersion(any(), any()) } returns "123"
        coEvery { applicationDeploymentService.deleteApplicationDeployments(any(), any()) } returns listOf(
            BooberDeleteResponse(BooberApplicationRef("aurora-utv", "gobo"), true, "")
        )
        coEvery { applicationService.getApplicationDeployments(any()) } returns listOf(
            ApplicationDeploymentResource(
                "", "gobo", "aurora", "utv", "aurora-utv", StatusResource("ok", null, emptyList(), emptyList()),
                VersionResource("", null, null), null, Instant.now(), null
            )
        )
    }

    @Test
    fun `Mutate application deployment current version`() {
        val variables = mapOf(
            "input" to mapOf(
                "applicationDeploymentId" to "123"
            )
        )
        webTestClient.queryGraphQL(redeployWithCurrentVersionMutation, variables, "test-token").expectBody()
            .graphqlData("redeployWithCurrentVersion.applicationDeploymentId").isEqualTo("123")
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

    @Test
    fun `Delete application deployments`() {
        val input =
            DeleteApplicationDeploymentsInput(
                "aurora",
                listOf(ApplicationDeploymentRef("dev-utv", "gobo"))
            )

        webTestClient.queryGraphQL(deleteApplicationDeploymentsMutation, input, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("deleteApplicationDeployments[0].namespace").isEqualTo("aurora-utv")
            .graphqlDoesNotContainErrors()
    }
}
