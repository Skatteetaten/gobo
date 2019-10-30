package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentWithDbResourceBuilder
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrors
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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

    @MockkBean
    private lateinit var databaseSchemaService: DatabaseSchemaServiceBlocking

    @MockkBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockkBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        every { databaseSchemaService.getDatabaseSchemas("paas") } returns listOf(DatabaseSchemaResourceBuilder().build())
        every { databaseSchemaService.getDatabaseSchema("myDbId") } returns DatabaseSchemaResourceBuilder().build()
        every { applicationService.getApplicationDeploymentsForDatabases("test-token", listOf("123")) } returns
            listOf(ApplicationDeploymentWithDbResourceBuilder(databaseId = "123").build())
        every { openShiftUserLoader.findOpenShiftUserByToken(any()) } returns OpenShiftUserBuilder().build()
    }

    @AfterEach
    fun tearDown() = clearAllMocks()

    @Test
    fun `Query for database schemas with no bearer token`() {
        val variables = mapOf("affiliations" to listOf("paas"))
        webTestClient.queryGraphQL(
            queryResource = getDatabaseSchemasWithAffiliationQuery,
            variables = variables
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlErrorsFirst("message").isNotEmpty
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
            .graphqlDataWithPrefix("databaseSchemas[0]") {
                graphqlData("databaseEngine").isEqualTo("ORACLE")
                graphqlData("affiliation.name").isEqualTo("paas")
                graphqlData("createdBy").isEqualTo("abc123")
                graphqlData("applicationDeployments.length()").isEqualTo(1)
            }
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
