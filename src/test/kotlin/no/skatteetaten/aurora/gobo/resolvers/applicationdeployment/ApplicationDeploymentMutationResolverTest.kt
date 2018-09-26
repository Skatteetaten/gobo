package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.createQuery
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono

@GraphQLTest
class ApplicationDeploymentMutationResolverTest {
    @Value("classpath:graphql/redeployWithVersion.graphql")
    private lateinit var redeployWithVersionMutation: Resource

    @Value("classpath:graphql/refreshApplicationDeployment.graphql")
    private lateinit var refreshApplicationDeploymentMutation: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationUpgradeService: ApplicationUpgradeService

    @Test
    fun `Mutate application deployment version`() {
        given(applicationUpgradeService.upgrade("123", "1")).willReturn(Mono.empty())

        val variables = mapOf(
            "input" to mapOf(
                "applicationDeploymentId" to "123",
                "version" to "1"

            )
        )
        val query = createQuery(redeployWithVersionMutation, variables)
        webTestClient
            .post()
            .uri("/graphql")
            .body(BodyInserters.fromObject(query))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.redeployWithVersion").isNotEmpty
    }

    @Test
    fun `Mutate refresh application deployment`() {
        given(applicationUpgradeService.refreshApplicationDeployment("123")).willReturn("123")

        val variables = mapOf(
            "input" to mapOf(
                "applicationDeploymentId" to "123"
            )
        )
        val query = createQuery(refreshApplicationDeploymentMutation, variables)
        webTestClient
            .post()
            .uri("/graphql")
            .body(BodyInserters.fromObject(query))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.refreshApplicationDeployment").isNotEmpty
    }
}