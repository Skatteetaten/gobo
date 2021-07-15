package no.skatteetaten.aurora.gobo.graphql.database

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentWithDbResourceBuilder
import no.skatteetaten.aurora.gobo.DatabaseInstanceResourceBuilder
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.RestorableDatabaseSchemaBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentDataLoader
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsMissingToken
import no.skatteetaten.aurora.gobo.graphql.isTrue
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(DatabaseSchemaQuery::class, ApplicationDeploymentDataLoader::class)
class DatabaseSchemaQueryTest : GraphQLTestWithDbhAndSkap() {

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

    @MockkBean
    private lateinit var databaseService: DatabaseService

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @BeforeEach
    fun setUp() {
        val paasInstance = DatabaseInstanceResourceBuilder().build()
        val auroraInstance = DatabaseInstanceResourceBuilder(affiliation = "aurora").build()

        coEvery { databaseService.getDatabaseInstances() } returns listOf(paasInstance, auroraInstance)
        coEvery { databaseService.getDatabaseSchemas("paas") } returns listOf(
            DatabaseSchemaResourceBuilder(name = "test1").build(),
            DatabaseSchemaResourceBuilder(name = "test2").build(),
            DatabaseSchemaResourceBuilder(name = "test3").build(),
            DatabaseSchemaResourceBuilder(name = "test4").build(),
            DatabaseSchemaResourceBuilder(name = "test5").build()
        )
        coEvery { databaseService.getDatabaseSchema("myDbId") } returns DatabaseSchemaResourceBuilder().build()
        coEvery { databaseService.getRestorableDatabaseSchemas("aurora") } returns listOf(
            RestorableDatabaseSchemaBuilder().build()
        )
        coEvery { applicationService.getApplicationDeploymentsForDatabases("test-token", listOf("123")) } returns
            listOf(ApplicationDeploymentWithDbResourceBuilder(databaseId = "123").build())
    }

    @Test
    fun `Query for database schemas with no bearer token`() {
        val variables = mapOf("affiliations" to listOf("paas"), "pageSize" to 5)
        webTestClient.queryGraphQL(
            queryResource = getDatabaseSchemasWithAffiliationQuery,
            variables = variables
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlErrorsMissingToken()
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
            .graphqlDoesNotContainErrors()
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
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for database schemas given affiliation`() {
        val variables = mapOf("affiliations" to listOf("paas"), "pageSize" to 3, "after" to "dGVzdDE=")
        webTestClient.queryGraphQL(
            queryResource = getDatabaseSchemasWithAffiliationQuery,
            variables = variables,
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlData("databaseSchemas.edges.length()").isEqualTo(3)
            .graphqlData("databaseSchemas.totalCount").isEqualTo(5)
            .graphqlData("databaseSchemas.edges[0].cursor").isNotEmpty
            .graphqlDataWithPrefix("databaseSchemas.pageInfo") {
                graphqlData("endCursor").isNotEmpty
                graphqlData("hasNextPage").isTrue()
            }
            .graphqlDataWithPrefix("databaseSchemas.edges[0].node") {
                graphqlData("engine").isEqualTo("POSTGRES")
                graphqlData("affiliation.name").isEqualTo("paas")
                graphqlData("createdBy").isEqualTo("abc123")
                graphqlData("applicationDeployments.length()").isEqualTo(1)
            }
            .graphqlDoesNotContainErrors()
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
            .graphqlDoesNotContainErrors()
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
