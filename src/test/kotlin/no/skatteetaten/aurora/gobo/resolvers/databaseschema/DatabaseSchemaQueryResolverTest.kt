package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import no.skatteetaten.aurora.gobo.ApplicationDeploymentWithDbResourceBuilder
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrors
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class DatabaseSchemaQueryResolverTest {

    @Value("classpath:graphql/queries/getDatabaseSchemasWithAffiliation.graphql")
    private lateinit var getDatabaseSchemasWithAffiliationQuery: Resource

    @Value("classpath:graphql/queries/getDatabaseSchemaWithId.graphql")
    private lateinit var getDatabaseSchemaWithIdQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var databaseSchemaService: DatabaseSchemaServiceBlocking

    @MockBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        given(databaseSchemaService.getDatabaseSchemas("paas"))
            .willReturn(listOf(DatabaseSchemaResourceBuilder().build()))

        given(databaseSchemaService.getDatabaseSchema("myDbId"))
            .willReturn(DatabaseSchemaResourceBuilder().build())

        given(applicationService.getApplicationDeploymentsForDatabase("test-token", listOf("123"))).willReturn(
            listOf(
                ApplicationDeploymentWithDbResourceBuilder("123").build()
            )
        )

        given(openShiftUserLoader.findOpenShiftUserByToken(anyString()))
            .willReturn(OpenShiftUserBuilder().build())
    }

    @AfterEach
    fun tearDown() = Mockito.reset(databaseSchemaService, openShiftUserLoader)

    @Test
    fun `Query for database schemas with no bearer token`() {
        val variables = mapOf("affiliations" to listOf("paas"))
        webTestClient.queryGraphQL(
            queryResource = getDatabaseSchemasWithAffiliationQuery,
            variables = variables
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlErrors("[0].message").isNotEmpty
    }

    @Test
    fun `Query for database schemas given affiliation`() {
        val variables = mapOf("affiliations" to listOf("paas"))
        webTestClient.queryGraphQL(
            queryResource = getDatabaseSchemasWithAffiliationQuery,
            variables = variables,
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlData("databaseSchemas.length()").isEqualTo(1)
            .graphqlData("databaseSchemas[0].databaseEngine").isEqualTo("ORACLE")
            .graphqlData("databaseSchemas[0].affiliation.name").isEqualTo("paas")
            .graphqlData("databaseSchemas[0].createdBy").isEqualTo("abc123")
            .graphqlData("databaseSchemas[0].applicationDeployments.length()").isEqualTo(1)
    }

    @Test
    fun `Query for database schema given no id`() {
        webTestClient.queryGraphQL(queryResource = getDatabaseSchemaWithIdQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("databaseSchema.databaseEngine").doesNotExist()
            .graphqlErrors("length()").isEqualTo(1)
    }

    @Test
    fun `Query for database schema given id`() {
        val variables = mapOf("id" to "myDbId")
        webTestClient.queryGraphQL(
            queryResource = getDatabaseSchemaWithIdQuery,
            variables = variables,
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlData("databaseSchema.databaseEngine").isEqualTo("ORACLE")
            .graphqlData("databaseSchema.applicationDeployments.length()").isEqualTo(1)
    }
}