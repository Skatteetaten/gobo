package no.skatteetaten.aurora.gobo.graphql.database

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithoutDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(DatabaseSchemaQuery::class)
class DatabaseSchemaQueryWithoutDbhTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/queries/getDatabaseInstances.graphql")
    private lateinit var getDatabaseInstancesQuery: Resource

    @MockkBean
    private lateinit var databaseService: DatabaseService

    @BeforeEach
    fun setUp() {
        coEvery { databaseService.getDatabaseInstances() } throws IntegrationDisabledException("DBH integration is disabled for this environment")
    }

    @Test
    fun `Query for database instances returns error message`() {
        webTestClient.queryGraphQL(
            queryResource = getDatabaseInstancesQuery,
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlErrorsFirst("message")
            .isEqualTo("Exception while fetching data (/databaseInstances) : DBH integration is disabled for this environment")
    }
}
