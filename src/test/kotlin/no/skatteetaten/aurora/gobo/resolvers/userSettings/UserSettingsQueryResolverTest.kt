package no.skatteetaten.aurora.gobo.resolvers.userSettings

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class UserSettingsQueryResolverTest {

    @Value("classpath:graphql/getUserSettings.graphql")
    private lateinit var getUserSettingsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `Query for current user`() {
        webTestClient
            .queryGraphQL(getUserSettingsQuery, mapOf("affiliation" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.userSettings.applicationDeploymentFilters[0].affiliation").isNotEmpty
    }
}