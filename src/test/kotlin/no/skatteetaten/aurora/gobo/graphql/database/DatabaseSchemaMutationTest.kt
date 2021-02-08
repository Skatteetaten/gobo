package no.skatteetaten.aurora.gobo.graphql.database

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.JdbcUserBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsMissingToken
import no.skatteetaten.aurora.gobo.graphql.isTrue
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseService
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaCooldownChangeResponse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(DatabaseSchemaMutation::class)
class DatabaseSchemaMutationTest : GraphQLTestWithDbhAndSkap() {
    @Value("classpath:graphql/mutations/updateDatabaseSchema.graphql")
    private lateinit var updateDatabaseSchemaMutation: Resource

    @Value("classpath:graphql/mutations/deleteDatabaseSchemas.graphql")
    private lateinit var deleteDatabaseSchemasMutation: Resource

    @Value("classpath:graphql/mutations/restoreDatabaseSchemas.graphql")
    private lateinit var restoreDatabaseSchemasMutation: Resource

    @Value("classpath:graphql/mutations/testJdbcConnectionForJdbcUser.graphql")
    private lateinit var testJdbcConnectionForJdbcUserMutation: Resource

    @Value("classpath:graphql/mutations/testJdbcConnectionForId.graphql")
    private lateinit var testJdbcConnectionForIdMutation: Resource

    @Value("classpath:graphql/mutations/createDatabaseSchema.graphql")
    private lateinit var createDatabaseSchemaMutation: Resource

    @MockkBean
    private lateinit var databaseSchemaService: DatabaseService

    private val updateInput = UpdateDatabaseSchemaInput(
        discriminator = "db1",
        createdBy = "user",
        description = "my db schema",
        id = "1234",
        affiliation = "paas",
        application = "application",
        environment = "test"
    )

    private val creationInput = CreateDatabaseSchemaInput(
        discriminator = "db1",
        createdBy = "user",
        description = "my db schema",
        affiliation = "paas",
        application = "application",
        environment = "test",
        engine = "ORACLE"
    )

    private val connectionInput = CreateDatabaseSchemaInput(
        jdbcUser = JdbcUser("user", "pass", "url"),
        discriminator = "db1",
        createdBy = "user",
        description = "my db schema",
        affiliation = "paas",
        application = "application",
        environment = "test",
        engine = "ORACLE"
    )

    @Test
    fun `Mutate database schema return true given response success`() {
        coEvery { databaseSchemaService.updateDatabaseSchema(any()) } returns DatabaseSchemaResourceBuilder().build()
        webTestClient.queryGraphQL(
            queryResource = updateDatabaseSchemaMutation,
            input = updateInput,
            token = "test-token"
        )
            .expectBody()
            .graphqlData("updateDatabaseSchema.id").isEqualTo("123")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Delete database schema given ids`() {
        coEvery { databaseSchemaService.deleteDatabaseSchemas(any()) } returns
            listOf(
                SchemaCooldownChangeResponse(id = "abc123", success = true),
                SchemaCooldownChangeResponse(id = "bcd234", success = false)
            )

        val deleteInput = DeleteDatabaseSchemasInput(listOf("abc123", "bcd234"))
        webTestClient.queryGraphQL(
            queryResource = deleteDatabaseSchemasMutation,
            input = deleteInput,
            token = "test-token"
        )
            .expectBody()
            .graphqlDataWithPrefix("deleteDatabaseSchemas") {
                graphqlData("succeeded.length()").isEqualTo(1)
                graphqlData("succeeded[0]").isEqualTo("abc123")
                graphqlData("failed.length()").isEqualTo(1)
                graphqlData("failed[0]").isEqualTo("bcd234")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Restore database schema given ids`() {
        coEvery { databaseSchemaService.restoreDatabaseSchemas(any()) } returns
            listOf(
                SchemaCooldownChangeResponse(id = "abc123", success = true),
                SchemaCooldownChangeResponse(id = "bcd234", success = false)
            )

        val restoreInput = RestoreDatabaseSchemasInput(listOf("abc123", "bcd234"), active = true)
        webTestClient.queryGraphQL(
            queryResource = restoreDatabaseSchemasMutation,
            input = restoreInput,
            token = "test-token"
        )
            .expectBody()
            .graphqlData("restoreDatabaseSchemas.succeeded.length()").isEqualTo(1)
            .graphqlData("restoreDatabaseSchemas.succeeded[0]").isEqualTo("abc123")
            .graphqlData("restoreDatabaseSchemas.failed.length()").isEqualTo(1)
            .graphqlData("restoreDatabaseSchemas.failed[0]").isEqualTo("bcd234")
    }

    @Test
    fun `Test JDBC connection for jdbcUser`() {
        coEvery { databaseSchemaService.testJdbcConnection(any<JdbcUser>()) } returns ConnectionVerificationResponse(
            hasSucceeded = true
        )
        webTestClient.queryGraphQL(
            queryResource = testJdbcConnectionForJdbcUserMutation,
            input = JdbcUserBuilder().build(),
            token = "test-token"
        )
            .expectBody()
            .graphqlData("testJdbcConnectionForJdbcUser.hasSucceeded").isTrue()
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Test JDBC connection for id`() {
        coEvery { databaseSchemaService.testJdbcConnection(any<String>()) } returns ConnectionVerificationResponse(
            hasSucceeded = true
        )
        webTestClient.queryGraphQL(
            queryResource = testJdbcConnectionForIdMutation,
            variables = mapOf("id" to "123"),
            token = "test-token"
        )
            .expectBody()
            .graphqlData("testJdbcConnectionForId.hasSucceeded").isTrue()
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Test JDBC connection for id without token`() {
        webTestClient.queryGraphQL(
            queryResource = testJdbcConnectionForIdMutation,
            variables = mapOf("id" to "123")
        )
            .expectBody()
            .graphqlErrorsMissingToken()
    }

    @Test
    fun `Create database schema`() {
        coEvery { databaseSchemaService.createDatabaseSchema(any()) } returns DatabaseSchemaResourceBuilder().build()
        webTestClient.queryGraphQL(
            queryResource = createDatabaseSchemaMutation,
            input = creationInput,
            token = "test-token"
        )
            .expectBody()
            .graphqlData("createDatabaseSchema.id").isEqualTo("123")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Create connection between existing database schema and dbh`() {
        coEvery { databaseSchemaService.createDatabaseSchema(any()) } returns DatabaseSchemaResourceBuilder().build()
        webTestClient.queryGraphQL(
            queryResource = createDatabaseSchemaMutation,
            input = connectionInput,
            token = "test-token"
        )
            .expectBody()
            .graphqlData("createDatabaseSchema.id").isEqualTo("123")
            .graphqlDoesNotContainErrors()
    }
}
