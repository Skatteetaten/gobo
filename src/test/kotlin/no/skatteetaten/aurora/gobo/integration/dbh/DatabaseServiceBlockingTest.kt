package no.skatteetaten.aurora.gobo.integration.dbh

import assertk.Assert
import assertk.assertThat
import assertk.assertions.cause
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.message
import assertk.assertions.support.expected
import com.jayway.jsonpath.JsonPath
import io.mockk.every
import io.mockk.mockk
import java.net.UnknownHostException
import no.skatteetaten.aurora.gobo.DatabaseInstanceResourceBuilder
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.JdbcUserBuilder
import no.skatteetaten.aurora.gobo.RestorableDatabaseSchemaBuilder
import no.skatteetaten.aurora.gobo.SchemaCreationRequestBuilder
import no.skatteetaten.aurora.gobo.SchemaDeletionRequestBuilder
import no.skatteetaten.aurora.gobo.SchemaRestorationRequestBuilder
import no.skatteetaten.aurora.gobo.SchemaUpdateRequestBuilder
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.containsAuroraToken
import no.skatteetaten.aurora.gobo.integration.containsAuroraTokens
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseServiceReactive.Companion.HEADER_COOLDOWN_DURATION_HOURS
import no.skatteetaten.aurora.gobo.resolvers.database.ConnectionVerificationResponse
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import no.skatteetaten.aurora.gobo.testObjectMapper
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.bodyAsObject
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.bodyAsString
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.create

class DatabaseServiceBlockingTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "abc123"
    }
    private val databaseService =
        DatabaseServiceBlocking(
            DatabaseServiceReactive(
                sharedSecretReader,
                create(server.url("/").toString()),
                testObjectMapper()
            )
        )

    @Test
    fun `Get database instances for affiliation`() {
        val response = DbhResponse.ok(DatabaseInstanceResourceBuilder().build())
        val request = server.execute(response) {
            val instances = databaseService.getDatabaseInstances()
            assertThat(instances).hasSize(1)
        }.first()
        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().contains("databaseInstance")
    }

    @Test
    fun `Get database schemas given affiliation`() {
        val response = DbhResponse.ok(DatabaseSchemaResourceBuilder().build())
        val request = server.execute(response) {
            val databaseSchemas = databaseService.getDatabaseSchemas("paas")
            assertThat(databaseSchemas).hasSize(1)
        }.first()
        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().contains("affiliation")
        assertThat(request?.path).isNotNull().contains("paas")
    }

    @Test
    fun `Get restorable database schemas given affiliation`() {
        val response = DbhResponse.ok(RestorableDatabaseSchemaBuilder().build())
        val request = server.execute(response) {
            val databaseSchemas = databaseService.getRestorableDatabaseSchemas("paas")
            assertThat(databaseSchemas).hasSize(1)
        }.first()
        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().contains("affiliation")
        assertThat(request?.path).isNotNull().contains("paas")
    }

    @Test
    fun `Get database schemas with required labels given affiliation`() {
        val labelsMissingEnv = mapOf(
            "affiliation" to "paas",
            "application" to "referanse",
            "userId" to "abc123",
            "name" to "ref"
        )
        val databaseSchema = DatabaseSchemaResourceBuilder().build()
        val response = DbhResponse.ok(databaseSchema, databaseSchema)
        val json = JsonPath.parse(testObjectMapper().writeValueAsString(response))
            .set("$.items[0].labels", labelsMissingEnv).jsonString()

        val request = server.execute(json) {
            val databaseSchemas = databaseService.getDatabaseSchemas("paas")
            assertThat(databaseSchemas).hasSize(1)
        }.first()
        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().contains("affiliation")
        assertThat(request?.path).isNotNull().contains("paas")
    }

    @Test
    fun `Get database schemas return failed response`() {
        val response = DbhResponse.failed("test error")
        server.execute(response) {
            assertThat {
                databaseService.getDatabaseSchemas("paas")
            }.isNotNull().isFailure().isInstanceOf(SourceSystemException::class)
                .message().isEqualTo("status=Failed error=test error")
        }
    }

    @Test
    fun `Get database schema given id`() {
        val response = DbhResponse.ok(DatabaseSchemaResourceBuilder().build())
        val request = server.execute(response) {
            val databaseSchema = databaseService.getDatabaseSchema("abc123")
            assertThat(databaseSchema).isNotNull()
        }.first()
        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().endsWith("/abc123")
    }

    @Test
    fun `Get database schema given non-existing id return failed`() {
        val response = DbhResponse.failed("test message")
        val request = server.execute(response) {
            assertThat { databaseService.getDatabaseSchema("abc123") }
                .isNotNull().isFailure().isInstanceOf(SourceSystemException::class)
                .hasMessage("status=Failed error=test message")
        }.first()
        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().endsWith("/abc123")
    }

    @Test
    fun `Update database schema`() {
        val response = DbhResponse.ok(DatabaseSchemaResourceBuilder().build())
        val request = server.execute(response) {
            val databaseSchema =
                databaseService.updateDatabaseSchema(SchemaUpdateRequestBuilder("123").build())
            assertThat(databaseSchema).isNotNull()
        }.first()
        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().endsWith("/123")
    }

    @Test
    fun `Delete database schema without cooldownDurationHours`() {
        val ok = DbhResponse.ok<DatabaseSchemaResource>()
        val failed = DbhResponse.failed()
        val requests = server.execute(ok, ok, failed) {
            val deletionRequests = listOf(
                SchemaDeletionRequestBuilder(id = "ok1").build(),
                SchemaDeletionRequestBuilder(id = "ok2").build(),
                SchemaDeletionRequestBuilder(id = "failed").build()
            )
            val deleted = databaseService.deleteDatabaseSchemas(deletionRequests)
            assertThat(deleted.size).isEqualTo(3)
            assertThat(deleted).succeeded(2)
            assertThat(deleted).failed(1)
        }
        assertThat(requests).containsAuroraTokens()
        assertThat(requests).containsPath("/ok1")
        assertThat(requests).containsPath("/ok2")
        assertThat(requests).containsPath("/failed")
        assertThat(requests.first()?.headers?.get(HEADER_COOLDOWN_DURATION_HOURS)).isNull()
    }

    @Test
    fun `Delete database schema with cooldownDurationHours`() {
        val response = DbhResponse.ok<DatabaseSchemaResource>()
        val request = server.execute(response) {
            val deleted = databaseService.deleteDatabaseSchemas(
                listOf(SchemaDeletionRequestBuilder(id = "123", cooldownDurationHours = 2).build())
            )
            assertThat(deleted.size).isEqualTo(1)
        }.first()
        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().endsWith("/123")
        assertThat(request?.headers?.get(HEADER_COOLDOWN_DURATION_HOURS)).isEqualTo("2")
    }

    @Test
    fun `Delete database schema ser ut error response`() {
        server.execute(404 to DbhResponse.failed()) {
            assertThat {
                databaseService.deleteDatabaseSchemas(listOf(SchemaDeletionRequestBuilder().build()))
            }.isNotNull().isFailure().isInstanceOf(SourceSystemException::class)
        }
    }

    @Test
    fun `Restore database schema fails with active set to false`() {
        val failed = DbhResponse.failed()
        val requests = server.execute(failed) {
            val restorationRequests = listOf(
                    SchemaRestorationRequestBuilder(id = "failed", active = false).build()
            )
            val restored = databaseService.restoreDatabaseSchemas(restorationRequests)
            assertThat(restored.size).isEqualTo(1)
            assertThat(restored).failed(1)
        }
        assertThat(requests).containsAuroraTokens()
        assertThat(requests).containsPath("/failed")
        assertThat(requests.first()?.bodyAsString()).isNotNull().contains("\"active\":false")
    }

    @Test
    fun `Restore database schema succeeds with active set to true`() {
        val response = DbhResponse.ok<DatabaseSchemaResource>()
        val request = server.execute(response) {
            val restored = databaseService.restoreDatabaseSchemas(
                    listOf(SchemaRestorationRequestBuilder(id = "123", active = true).build())
            )
            assertThat(restored.size).isEqualTo(1)
        }.first()
        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().endsWith("/123")
        assertThat(request?.bodyAsString()).isNotNull().contains("\"active\":true")
    }

    @Test
    fun `Test jdbc connection for jdbcUser`() {
        val jdbcUser = JdbcUserBuilder().build()
        val response = DbhResponse.ok(ConnectionVerificationResponse(hasSucceeded = true))
        val request = server.execute(response) {
            val success = databaseService.testJdbcConnection(jdbcUser)
            assertThat(success.hasSucceeded).isTrue()
        }.first()

        val requestJdbcUser = request?.bodyAsObject<JdbcUser>("$.jdbcUser")

        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().endsWith("/validate")
        assertThat(requestJdbcUser).isEqualTo(jdbcUser)
    }

    @Test
    fun `Test jdbc connection for id`() {
        val response = DbhResponse.ok(ConnectionVerificationResponse(hasSucceeded = true))
        val request = server.execute(response) {
            val success = databaseService.testJdbcConnection("123")
            assertThat(success.hasSucceeded).isTrue()
        }.first()

        val requestId = request?.bodyAsObject<String>("$.id")

        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().endsWith("/validate")
        assertThat(requestId).isEqualTo("123")
    }

    @Test
    fun `Test jdbc connection for id given failing connection`() {
        val response = DbhResponse.ok(ConnectionVerificationResponse(hasSucceeded = false))
        server.execute(response) {
            val success = databaseService.testJdbcConnection("123")
            assertThat(success.hasSucceeded).isFalse()
        }
    }

    @Test
    fun `Create database schema`() {
        val response = DbhResponse.ok(DatabaseSchemaResourceBuilder().build())
        val request = server.execute(response) {
            val createdDatabaseSchema =
                databaseService.createDatabaseSchema(SchemaCreationRequestBuilder().build())
            assertThat(createdDatabaseSchema.id).isEqualTo("123")
        }.first()

        val creationRequest = request?.bodyAsObject<SchemaCreationRequest>()

        assertThat(request).containsAuroraToken()
        assertThat(request?.path).isNotNull().endsWith("/")
        assertThat(creationRequest?.jdbcUser).isNotNull()
    }

    @Test
    fun `Get database schema given unknown hostname throw SourceSystemException`() {
        val serviceWithUnknownHost =
            DatabaseServiceBlocking(
                DatabaseServiceReactive(
                    sharedSecretReader,
                    WebClient.create("http://unknown-hostname"),
                    testObjectMapper()
                )
            )

        assertThat { serviceWithUnknownHost.getDatabaseSchema("abc123") }
            .isNotNull().isFailure().isInstanceOf(SourceSystemException::class)
            .cause().isNotNull().isInstanceOf(UnknownHostException::class)
    }

    private fun Assert<List<RecordedRequest?>>.containsPath(endingWith: String) = given { requests ->
        val values = requests.filterNotNull().any { it.path?.endsWith(endingWith) ?: false }
        if (values) return
        expected("Requests to end with path $endingWith")
    }

    private fun Assert<List<SchemaCooldownChangeResponse>>.succeeded(count: Int) = given { responses ->
        if (responses.filter { it.success }.size == count) return
        expected("Succeeded responses size to equal $count")
    }

    private fun Assert<List<SchemaCooldownChangeResponse>>.failed(count: Int) = given { responses ->
        if (responses.filter { !it.success }.size == count) return
        expected("Failed responses size to equal $count")
    }
}
