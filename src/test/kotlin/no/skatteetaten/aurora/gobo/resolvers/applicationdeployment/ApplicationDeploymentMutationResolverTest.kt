package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationRef
import no.skatteetaten.aurora.gobo.integration.boober.DeleteApplicationDeploymentsInput
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

    @Value("classpath:graphql/mutations/deleteApplicationDeployments.graphql")
    private lateinit var deleteApplicationDeploymentsMutation: Resource

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
        val input = DeleteApplicationDeploymentsInput(listOf(ApplicationRef("aurora-dev", "konsoll")))
        val variables = jacksonObjectMapper().convertValue<Map<String, Any>>(input)

        webTestClient.queryGraphQL(deleteApplicationDeploymentsMutation, mapOf("input" to variables))
            .expectStatus().isOk
            .expectBody()
            .graphqlData("deleteApplicationDeployments").isTrue()
    }
}