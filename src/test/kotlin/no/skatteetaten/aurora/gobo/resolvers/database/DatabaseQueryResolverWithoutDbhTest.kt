package no.skatteetaten.aurora.gobo.resolvers.database

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.resolvers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class DatabaseQueryResolverWithoutDbhTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/queries/getDatabaseInstances.graphql")
    private lateinit var getDatabaseInstancesQuery: Resource

    @MockkBean
    private lateinit var databaseService: DatabaseServiceReactive

    @BeforeEach
    fun setUp() {
        every { databaseService.getDatabaseInstances() } throws IntegrationDisabledException("DBH integration is disabled for this environment")
    }

    @Disabled("Implement errror handling")
    @Test
    fun `Query for database instances returns error message`() {
        webTestClient.queryGraphQL(
            queryResource = getDatabaseInstancesQuery,
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlErrorsFirst("message").isEqualTo("DBH integration is disabled for this environment")
    }
}
