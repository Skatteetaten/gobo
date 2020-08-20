package no.skatteetaten.aurora.gobo.resolvers.database

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithoutDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Disabled
class DatabaseQueryResolverWithoutDbhTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/queries/getDatabaseInstances.graphql")
    private lateinit var getDatabaseInstancesQuery: Resource

    @MockkBean
    private lateinit var databaseService: DatabaseService

    @BeforeEach
    fun setUp() {
        every { databaseService.getDatabaseInstances() } throws IntegrationDisabledException("DBH integration is disabled for this environment")
    }

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
