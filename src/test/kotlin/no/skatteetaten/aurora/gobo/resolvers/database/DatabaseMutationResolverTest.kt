package no.skatteetaten.aurora.gobo.resolvers.database

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.JdbcUserBuilder
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaCooldownChangeResponse
import no.skatteetaten.aurora.gobo.resolvers.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

class DatabaseMutationResolverTest : GraphQLTestWithDbhAndSkap() {
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
    private lateinit var databaseSchemaService: DatabaseServiceReactive

    private val updateVariables = mapOf(
        "input" to jacksonObjectMapper().convertValue<Map<String, Any>>(
            UpdateDatabaseSchemaInput(
                discriminator = "db1",
                createdBy = "user",
                description = "my db schema",
                id = "1234",
                affiliation = "paas",
                application = "application",
                environment = "test"
            )
        )
    )

    private val creationVariables = mapOf(
        "input" to jacksonObjectMapper().convertValue<Map<String, Any>>(
            CreateDatabaseSchemaInput(
                discriminator = "db1",
                createdBy = "user",
                description = "my db schema",
                affiliation = "paas",
                application = "application",
                environment = "test",
                engine = "ORACLE"
            )
        )
    )

    private val connectionVariables = mapOf(
        "input" to jacksonObjectMapper().convertValue<Map<String, Any>>(
            CreateDatabaseSchemaInput(
                jdbcUser = JdbcUser("user", "pass", "url"),
                discriminator = "db1",
                createdBy = "user",
                description = "my db schema",
                affiliation = "paas",
                application = "application",
                environment = "test",
                engine = "ORACLE"
            )
        )
    )

    @Test
    fun `Mutate database schema return true given response success`() {
        every { databaseSchemaService.updateDatabaseSchema(any()) } returns DatabaseSchemaResourceBuilder().build().toMono()
        webTestClient.queryGraphQL(
            queryResource = updateDatabaseSchemaMutation,
            variables = updateVariables,
            token = "test-token"
        )
            .expectBody()
            .graphqlData("updateDatabaseSchema.id").isEqualTo("123")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Delete database schema given ids`() {
        every { databaseSchemaService.deleteDatabaseSchemas(any()) } returns
            listOf(
                SchemaCooldownChangeResponse(id = "abc123", success = true),
                SchemaCooldownChangeResponse(id = "bcd234", success = false)
            ).toFlux()

        val request = DeleteDatabaseSchemasInput(listOf("abc123", "bcd234"))
        val deleteVariables = mapOf("input" to jacksonObjectMapper().convertValue<Map<String, Any>>(request))

        webTestClient.queryGraphQL(
            queryResource = deleteDatabaseSchemasMutation,
            variables = deleteVariables,
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
        every { databaseSchemaService.restoreDatabaseSchemas(any()) } returns
            listOf(
                SchemaCooldownChangeResponse(id = "abc123", success = true),
                SchemaCooldownChangeResponse(id = "bcd234", success = false)
            ).toFlux()

        val request = RestoreDatabaseSchemasInput(listOf("abc123", "bcd234"), active = true)
        val restoreVariables = mapOf("input" to jacksonObjectMapper().convertValue<Map<String, Any>>(request))

        webTestClient.queryGraphQL(
            queryResource = restoreDatabaseSchemasMutation,
            variables = restoreVariables,
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
        every { databaseSchemaService.testJdbcConnection(any(),any()) } returns ConnectionVerificationResponse(hasSucceeded = true).toMono()
        val variables =
            mapOf("input" to jacksonObjectMapper().convertValue<Map<String, Any>>(JdbcUserBuilder().build()))
        webTestClient.queryGraphQL(
            queryResource = testJdbcConnectionForJdbcUserMutation,
            variables = variables,
            token = "test-token"
        )
            .expectBody()
            .graphqlData("testJdbcConnectionForJdbcUser.hasSucceeded").isTrue()
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Test JDBC connection for id`() {
        every { databaseSchemaService.testJdbcConnection(any<String>()) } returns ConnectionVerificationResponse(hasSucceeded = true).toMono()
        webTestClient.queryGraphQL(
            queryResource = testJdbcConnectionForIdMutation,
            variables = mapOf("id" to "123"),
            token = "test-token"
        )
            .expectBody()
            .graphqlData("testJdbcConnectionForId.hasSucceeded").isTrue()
            .graphqlDoesNotContainErrors()
    }

    @Disabled("Autentication not implemented")
    @Test
    fun `Test JDBC connection for id without token`() {
        webTestClient.queryGraphQL(
            queryResource = testJdbcConnectionForIdMutation,
            variables = mapOf("id" to "123")
        )
            .expectBody()
            .graphqlErrorsFirst("[?(@.message =~ /.*Anonymous user cannot test jdbc connection/)]")
            .isNotEmpty
    }

    @Test
    fun `Create database schema`() {
        every { databaseSchemaService.createDatabaseSchema(any()) } returns DatabaseSchemaResourceBuilder().build().toMono()
        webTestClient.queryGraphQL(
            queryResource = createDatabaseSchemaMutation,
            variables = creationVariables,
            token = "test-token"
        )
            .expectBody()
            .graphqlData("createDatabaseSchema.id").isEqualTo("123")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Create connection between existing database schema and dbh`() {
        every { databaseSchemaService.createDatabaseSchema(any()) } returns DatabaseSchemaResourceBuilder().build().toMono()
        webTestClient.queryGraphQL(
            queryResource = createDatabaseSchemaMutation,
            variables = connectionVariables,
            token = "test-token"
        )
            .expectBody()
            .graphqlData("createDatabaseSchema.id").isEqualTo("123")
            .graphqlDoesNotContainErrors()
    }
}
