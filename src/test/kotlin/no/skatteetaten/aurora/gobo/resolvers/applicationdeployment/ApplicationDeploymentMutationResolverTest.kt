package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import no.skatteetaten.aurora.gobo.GraphQLTest
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
    @Value("classpath:graphql/redeployWithVersion.graphql")
    private lateinit var redeployWithVersionMutation: Resource

    @Value("classpath:graphql/refreshApplicationDeployment.graphql")
    private lateinit var refreshApplicationDeploymentByDeploymentIdMutation: Resource

    @Value("classpath:graphql/refreshApplicationDeployments.graphql")
    private lateinit var refreshApplicationDeploymentsByAffiliationsMutation: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationUpgradeService: ApplicationUpgradeService

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
            .jsonPath("$.data.redeployWithVersion").isNotEmpty
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
            .jsonPath("$.data.refreshApplicationDeployment").isNotEmpty
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
            .jsonPath("$.data.refreshApplicationDeployments").isNotEmpty
    }
}