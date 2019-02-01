package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.reset
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.JdbcUserBuilder
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceBlocking
import no.skatteetaten.aurora.gobo.integration.dbh.JdbcUser
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class DatabaseSchemaMutationResolverTest {
    @Value("classpath:graphql/updateDatabaseSchema.graphql")
    private lateinit var updateDatabaseSchemaMutation: Resource

    @Value("classpath:graphql/deleteDatabaseSchema.graphql")
    private lateinit var deleteDatabaseSchemaMutation: Resource

    @Value("classpath:graphql/testJdbcConnectionForJdbcUser.graphql")
    private lateinit var testJdbcConnectionForJdbcUserMutation: Resource

    @Value("classpath:graphql/testJdbcConnectionForId.graphql")
    private lateinit var testJdbcConnectionForIdMutation: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var databaseSchemaService: DatabaseSchemaServiceBlocking

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    private val variables = mapOf(
        "input" to jacksonObjectMapper().convertValue<Map<String, Any>>(
            DatabaseSchemaInput(
                discriminator = "db1",
                userId = "user",
                description = "my db schema",
                id = "1234",
                affiliation = "paas",
                application = "application",
                environment = "test"
            )
        )
    )

    @BeforeEach
    fun setUp() {
        given(openShiftUserLoader.findOpenShiftUserByToken(BDDMockito.anyString()))
            .willReturn(OpenShiftUserBuilder().build())
    }

    @AfterEach
    fun tearDown() = reset(databaseSchemaService)

    @Test
    fun `Mutate database schema return true given response success`() {
        given(databaseSchemaService.updateDatabaseSchema(any())).willReturn(true)
        webTestClient.queryGraphQL(
            queryResource = updateDatabaseSchemaMutation,
            variables = variables,
            token = "test-token"
        )
            .expectBody()
            .jsonPath("$.data.updateDatabaseSchema").isEqualTo(true)
    }

    @Test
    fun `Mutate database schema return false given response failure`() {
        given(databaseSchemaService.updateDatabaseSchema(any())).willReturn(false)
        webTestClient.queryGraphQL(
            queryResource = updateDatabaseSchemaMutation,
            variables = variables,
            token = "test-token"
        )
            .expectBody()
            .jsonPath("$.data.updateDatabaseSchema").isEqualTo(false)
    }

    @Test
    fun `Delete database schema given id`() {
        given(databaseSchemaService.deleteDatabaseSchema(any())).willReturn(true)
        val deleteVariables = mapOf("input" to mapOf("id" to "abc123"))
        webTestClient.queryGraphQL(
            queryResource = deleteDatabaseSchemaMutation,
            variables = deleteVariables,
            token = "test-token"
        )
            .expectBody()
            .jsonPath("$.data.deleteDatabaseSchema").isEqualTo(true)
    }

    @Test
    fun `Test JDBC connection for jdbcUser`() {
        given(databaseSchemaService.testJdbcConnection(any<JdbcUser>())).willReturn(true)
        val variables =
            mapOf("input" to jacksonObjectMapper().convertValue<Map<String, Any>>(JdbcUserBuilder().build()))
        webTestClient.queryGraphQL(
            queryResource = testJdbcConnectionForJdbcUserMutation,
            variables = variables,
            token = "test-token"
        )
            .expectBody()
            .jsonPath("$.data.testJdbcConnectionForJdbcUser").isEqualTo(true)
    }

    @Test
    fun `Test JDBC connection for id`() {
        given(databaseSchemaService.testJdbcConnection(any<String>())).willReturn(true)
        webTestClient.queryGraphQL(
            queryResource = testJdbcConnectionForIdMutation,
            variables = mapOf("id" to "123"),
            token = "test-token"
        )
            .expectBody()
            .jsonPath("$.data.testJdbcConnectionForId").isEqualTo(true)
    }
}