package no.skatteetaten.aurora.gobo.resolvers.user

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.createQuery
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@GraphQLTest
class CurrentUserQueryResolverTest {

    @Value("classpath:graphql/getCurrentUser.graphql")
    private lateinit var getCurrentUserQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `Query for current user`() {
        val query = createQuery(getCurrentUserQuery)
        webTestClient
            .post()
            .uri("/graphql")
            .body(BodyInserters.fromObject(query))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.currentUser.id").isNotEmpty
            .jsonPath("$.data.currentUser.name").isNotEmpty
    }
}