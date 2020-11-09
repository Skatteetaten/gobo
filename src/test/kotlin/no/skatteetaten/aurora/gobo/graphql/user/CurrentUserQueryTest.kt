package no.skatteetaten.aurora.gobo.graphql.user

import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(CurrentUserQuery::class)
class CurrentUserQueryTest : GraphQLTestWithDbhAndSkap() {

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
