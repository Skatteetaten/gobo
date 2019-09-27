package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.nhaarman.mockito_kotlin.any
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.isTrue
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.anyList
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class ApplicationDeploymentMutationResolverTest {
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

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationUpgradeService: ApplicationUpgradeService

    @MockBean
    private lateinit var applicationDeploymentService: ApplicationDeploymentService

    @AfterEach
    fun tearDown() = reset(applicationUpgradeService)

    @Test
    fun `Mutate application deployment version`() {
        val variables = mapOf(
            "input" to mapOf(
                "applicationDeploymentId" to "123",
                "version" to "1"

            )
        )
        webTestClient.queryGraphQL(redeployWithVersionMutation, variables).expectBody()
            .graphqlData("redeployWithVersion").isNotEmpty
    }

    @Test
    fun `Mutate application deployment current version`() {
        val variables = mapOf(
            "input" to mapOf(
                "applicationDeploymentId" to "123"
            )
        )
        webTestClient.queryGraphQL(redeployWithCurrentVersionMutation, variables).expectBody()
            .graphqlData("redeployWithCurrentVersion").isNotEmpty
    }

    @Test
    fun `Mutate refresh application deployment by applicationDeploymentId`() {
        given(applicationUpgradeService.refreshApplicationDeployment(anyString(), anyString())).willReturn(true)

        val variables = mapOf(
            "input" to mapOf(
                "applicationDeploymentId" to "123"
            )
        )
        webTestClient.queryGraphQL(refreshApplicationDeploymentByDeploymentIdMutation, variables)
            .expectStatus().isOk
            .expectBody()
            .graphqlData("refreshApplicationDeployment").isNotEmpty
    }

    @Test
    fun `Mutate refresh application deployment by affiliations`() {
        given(applicationUpgradeService.refreshApplicationDeployments(anyString(), anyList())).willReturn(true)

        val variables = mapOf(
            "input" to mapOf(
                "affiliations" to listOf("aurora")
            )
        )
        webTestClient.queryGraphQL(refreshApplicationDeploymentsByAffiliationsMutation, variables)
            .expectStatus().isOk
            .expectBody()
            .graphqlData("refreshApplicationDeployments").isNotEmpty
    }

    @Test
    fun `Delete application deployment`() {
        given(applicationDeploymentService.deleteApplicationDeployment(anyString(), any())).willReturn(true)

        val variables = mapOf(
            "input" to mapOf(
                "namespace" to "aurora-dev",
                "name" to "konsoll"
            )
        )

        webTestClient.queryGraphQL(deleteApplicationDeploymentMutation, variables)
            .expectStatus().isOk
            .expectBody()
            .graphqlData("deleteApplicationDeployment").isTrue()
    }
}