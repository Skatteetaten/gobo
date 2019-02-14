package no.skatteetaten.aurora.gobo.resolvers.gobo

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class GoboQueryResolverTest {

    @Value("classpath:graphql/queries/getGoboUsage.graphql")
    private lateinit var getGoboUsageQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `Get Gobo usage`() {
        webTestClient.queryGraphQL(getGoboUsageQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlData("gobo.usage.usedFields").isNotEmpty
    }
}