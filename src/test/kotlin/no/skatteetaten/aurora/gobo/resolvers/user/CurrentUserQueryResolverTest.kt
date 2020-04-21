package no.skatteetaten.aurora.gobo.resolvers.user

import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class CurrentUserQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getCurrentUser.graphql")
    private lateinit var getCurrentUserQuery: Resource

    @Test
    fun `Query for current user`() {
        webTestClient
            .queryGraphQL(getCurrentUserQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlData("currentUser.id").isNotEmpty
            .graphqlData("currentUser.name").isNotEmpty
            .graphqlDoesNotContainErrors()
    }
}
