package no.skatteetaten.aurora.gobo.resolvers.user

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class CurrentUserQueryResolverTest {

    @Value("classpath:graphql/queries/getCurrentUser.graphql")
    private lateinit var getCurrentUserQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `Query for current user`() {
        webTestClient
            .queryGraphQL(getCurrentUserQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlData("currentUser.id").isNotEmpty
            .graphqlData("currentUser.name").isNotEmpty
    }
}