package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.createQuery
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@GraphQLTest
class ApplicationDeploymentMutationResolverTest {
    @Value("classpath:graphql/updateApplicationDeploymentVersion.graphql")
    private lateinit var updateVersionMutation: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `Mutate application deployment version`() {
        val variables = mapOf(
            "input" to mapOf(
                "affiliation" to "paas",
                "branch" to "master"

            )
        )
        val query = createQuery(updateVersionMutation, variables)
        webTestClient
            .post()
            .uri("/graphql")
            .body(BodyInserters.fromObject(query))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.updateApplicationDeploymentVersion.name").exists()
    }
}