package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import io.mockk.clearMocks
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class DatabaseSchemaMutationResolverTest {
    @Value("classpath:graphql/updateDatabaseSchema.graphql")
    private lateinit var updateDatabaseSchemaMutation: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var databaseSchemaService: DatabaseSchemaServiceBlocking

    @AfterEach
    fun tearDown() = clearMocks(databaseSchemaService)

    @Test
    fun `Mutate database schema`() {
        val variables = mapOf(
            "appDbName" to "db1",
            "username" to "user",
            "description" to "my db schema"
        )
        webTestClient.queryGraphQL(queryResource = updateDatabaseSchemaMutation, variables = variables)
            .expectBody()
            .jsonPath("$.data").isEqualTo(true)
    }
}