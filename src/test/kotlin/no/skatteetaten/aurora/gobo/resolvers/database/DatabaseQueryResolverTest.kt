package no.skatteetaten.aurora.gobo.resolvers.database

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentWithDbResourceBuilder
import no.skatteetaten.aurora.gobo.DatabaseInstanceResourceBuilder
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.RestorableDatabaseSchemaBuilder
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.AbstractGraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrors
import no.skatteetaten.aurora.gobo.resolvers.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource

class DatabaseQueryResolverTest : AbstractGraphQLTest() {

    @Value("classpath:graphql/queries/getDatabaseInstances.graphql")
    private lateinit var getDatabaseInstancesQuery: Resource

    @Value("classpath:graphql/queries/getDatabaseInstancesWithAffiliation.graphql")
    private lateinit var getDatabaseInstancesWithAffiliationQuery: Resource

    @Value("classpath:graphql/queries/getDatabaseSchemasWithAffiliation.graphql")
    private lateinit var getDatabaseSchemasWithAffiliationQuery: Resource

    @Value("classpath:graphql/queries/getDatabaseSchemaWithId.graphql")
    private lateinit var getDatabaseSchemaWithIdQuery: Resource

    @Value("classpath:graphql/queries/getRestorableDatabaseSchemas.graphql")
    private lateinit var getRestorableDatabaseSchemasQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var databaseService: DatabaseServiceBlocking

    @MockkBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        val paasInstance = DatabaseInstanceResourceBuilder().build()
        val auroraInstance = DatabaseInstanceResourceBuilder(affiliation = "aurora").build()

        every { databaseService.getDatabaseInstances() } returns listOf(paasInstance, auroraInstance)
        every { databaseService.getDatabaseSchemas("paas") } returns listOf(DatabaseSchemaResourceBuilder().build())
        every { databaseService.getDatabaseSchema("myDbId") } returns DatabaseSchemaResourceBuilder().build()
        every { applicationService.getApplicationDeploymentsForDatabases("test-token", listOf("123")) } returns
            listOf(ApplicationDeploymentWithDbResourceBuilder(databaseId = "123").build())

        given(databaseSchemaService.getRestorableDatabaseSchemas("aurora"))
            .willReturn(listOf(RestorableDatabaseSchemaBuilder().build()))
    }

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
    fun `Query for database instances`() {
        webTestClient.queryGraphQL(
            queryResource = getDatabaseInstancesQuery,
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlData("databaseInstances.length()").isEqualTo(2)
            .graphqlDataWithPrefix("databaseInstances[0]") {
                graphqlData("engine").isEqualTo("POSTGRES")
                graphqlData("instanceName").isEqualTo("name")
                graphqlData("labels[0].key").isEqualTo("affiliation")
            }
    }

    @Test
    fun `Query for database instances given affiliation`() {
        webTestClient.queryGraphQL(
            queryResource = getDatabaseInstancesWithAffiliationQuery,
            variables = mapOf("affiliation" to "paas"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlData("databaseInstances.length()").isEqualTo(1)
            .graphqlDataWithPrefix("databaseInstances[0]") {
                graphqlData("engine").isEqualTo("POSTGRES")
                graphqlData("instanceName").isEqualTo("name")
                graphqlData("affiliation.name").isEqualTo("paas")
            }
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
                graphqlData("engine").isEqualTo("POSTGRES")
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
            .graphqlData("databaseSchema.engine").doesNotExist()
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
            .graphqlData("databaseSchema.engine").isEqualTo("POSTGRES")
            .graphqlData("databaseSchema.applicationDeployments.length()").isEqualTo(1)
    }

    @Test
    fun `Query for restorable database schemas given affiliation`() {
        val variables = mapOf("affiliations" to listOf("aurora"))
        webTestClient.queryGraphQL(
            queryResource = getRestorableDatabaseSchemasQuery,
            variables = variables,
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("restorableDatabaseSchemas") {
                graphqlDataFirst("databaseSchema.application").isEqualTo("referanse")
                graphqlDataFirst("deleteAfter").isNotEmpty
                graphqlDataFirst("setToCooldownAt").isNotEmpty
            }
    }
}
