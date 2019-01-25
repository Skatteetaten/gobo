package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.reset
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceBlocking
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
            token = "test-token")
            .expectBody()
            .jsonPath("$.data.updateDatabaseSchema").isEqualTo(true)
    }

    @Test
    fun `Mutate database schema return false given response failure`() {
        given(databaseSchemaService.updateDatabaseSchema(any())).willReturn(false)
        webTestClient.queryGraphQL(
            queryResource = updateDatabaseSchemaMutation,
            variables = variables,
            token = "test-token")
            .expectBody()
            .jsonPath("$.data.updateDatabaseSchema").isEqualTo(false)
    }
}