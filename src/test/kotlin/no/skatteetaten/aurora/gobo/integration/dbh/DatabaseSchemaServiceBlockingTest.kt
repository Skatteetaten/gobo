package no.skatteetaten.aurora.gobo.integration.dbh

import assertk.Assert
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.message
import assertk.assertions.support.expected
import assertk.catch
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.JsonPath
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.JdbcUserBuilder
import no.skatteetaten.aurora.gobo.SchemaCreationRequestBuilder
import no.skatteetaten.aurora.gobo.SchemaDeletionRequestBuilder
import no.skatteetaten.aurora.gobo.SchemaUpdateRequestBuilder
import no.skatteetaten.aurora.gobo.integration.MockWebServerTestTag
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.bodyAsObject
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceReactive.Companion.HEADER_AURORA_TOKEN
import no.skatteetaten.aurora.gobo.integration.dbh.DatabaseSchemaServiceReactive.Companion.HEADER_COOLDOWN_DURATION_HOURS
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient.create

@MockWebServerTestTag
class DatabaseSchemaServiceBlockingTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "abc123"
    }
    private val databaseSchemaService =
        DatabaseSchemaServiceBlocking(DatabaseSchemaServiceReactive(sharedSecretReader, create(server.url("/").toString())))

    @Test
    fun `Get database schemas given affiliation`() {
        val response = Response(items = listOf(DatabaseSchemaResourceBuilder().build()))
        val request = server.execute(response) {
            val databaseSchemas = databaseSchemaService.getDatabaseSchemas("paas")
            assertThat(databaseSchemas).hasSize(1)
        }
        assertThat(request).containsAuroraToken()
        assertThat(request.path).contains("affiliation")
        assertThat(request.path).contains("paas")
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
        val response = Response(items = listOf(databaseSchema, databaseSchema))
        val json = JsonPath.parse(jacksonObjectMapper().writeValueAsString(response))
            .set("$.items[0].labels", labelsMissingEnv).jsonString()

        val request = server.execute(json) {
            val databaseSchemas = databaseSchemaService.getDatabaseSchemas("paas")
            assertThat(databaseSchemas).hasSize(1)
        }
        assertThat(request).containsAuroraToken()
        assertThat(request.path).contains("affiliation")
        assertThat(request.path).contains("paas")
    }

    @Test
    fun `Get database schemas return failed response`() {
        val response = Response<DatabaseSchemaResource>(message = "failed", success = false, items = emptyList())
        server.execute(response) {
            val exception = catch { databaseSchemaService.getDatabaseSchemas("paas") }
            assertThat(exception).isNotNull()
                .isInstanceOf(SourceSystemException::class)
                .message().isEqualTo("failed")
        }
    }

    @Test
    fun `Get database schema given id`() {
        val response = Response(items = listOf(DatabaseSchemaResourceBuilder().build()))
        val request = server.execute(response) {
            val databaseSchema = databaseSchemaService.getDatabaseSchema("abc123")
            assertThat(databaseSchema).isNotNull()
        }
        assertThat(request).containsAuroraToken()
        assertThat(request.path).endsWith("/abc123")
    }

    @Test
    fun `Update database schema`() {
        val response = Response(items = listOf(DatabaseSchemaResourceBuilder().build()))
        val request = server.execute(response) {
            val databaseSchema =
                databaseSchemaService.updateDatabaseSchema(SchemaUpdateRequestBuilder("123").build())
            assertThat(databaseSchema).isNotNull()
        }
        assertThat(request).containsAuroraToken()
        assertThat(request.path).endsWith("/123")
    }

    @Test
    fun `Delete database schema without cooldownDurationHours`() {
        val response = Response(items = emptyList<DatabaseSchemaResource>())
        val request = server.execute(response) {
            val deleted = databaseSchemaService.deleteDatabaseSchema(SchemaDeletionRequestBuilder(id = "123").build())
            assertThat(deleted).isTrue()
        }
        assertThat(request).containsAuroraToken()
        assertThat(request.path).endsWith("/123")
        assertThat(request.headers[HEADER_COOLDOWN_DURATION_HOURS]).isNull()
    }

    @Test
    fun `Delete database schema with cooldownDurationHours`() {
        val response = Response(items = emptyList<DatabaseSchemaResource>())
        val request = server.execute(response) {
            val deleted = databaseSchemaService.deleteDatabaseSchema(
                SchemaDeletionRequestBuilder(
                    id = "123",
                    cooldownDurationHours = 2
                ).build()
            )
            assertThat(deleted).isTrue()
        }
        assertThat(request).containsAuroraToken()
        assertThat(request.path).endsWith("/123")
        assertThat(request.headers[HEADER_COOLDOWN_DURATION_HOURS]).isEqualTo("2")
    }

    @Test
    fun `Test jdbc connection for jdbcUser`() {
        val jdbcUser = JdbcUserBuilder().build()
        val response = Response(items = listOf(true))
        val request = server.execute(response) {
            val success = databaseSchemaService.testJdbcConnection(jdbcUser)
            assertThat(success).isTrue()
        }

        val requestJdbcUser = request.bodyAsObject<JdbcUser>("$.jdbcUser")

        assertThat(request).containsAuroraToken()
        assertThat(request.path).endsWith("/validate")
        assertThat(requestJdbcUser).isEqualTo(jdbcUser)
    }

    @Test
    fun `Test jdbc connection for id`() {
        val response = Response(items = listOf(true))
        val request = server.execute(response) {
            val success = databaseSchemaService.testJdbcConnection("123")
            assertThat(success).isTrue()
        }

        val requestId = request.bodyAsObject<String>("$.id")

        assertThat(request).containsAuroraToken()
        assertThat(request.path).endsWith("/validate")
        assertThat(requestId).isEqualTo("123")
    }

    @Test
    fun `Test jdbc connection for id given failing connection`() {
        val response = Response(items = listOf(false))
        server.execute(response) {
            val success = databaseSchemaService.testJdbcConnection("123")
            assertThat(success).isFalse()
        }
    }

    @Test
    fun `Create database schema`() {
        val response = Response(items = listOf(DatabaseSchemaResourceBuilder().build()))
        val request = server.execute(response) {
            val createdDatabaseSchema =
                databaseSchemaService.createDatabaseSchema(SchemaCreationRequestBuilder().build())
            assertThat(createdDatabaseSchema.id).isEqualTo("123")
        }

        val creationRequest = request.bodyAsObject<SchemaCreationRequest>()

        assertThat(request).containsAuroraToken()
        assertThat(request.path).endsWith("/")
        assertThat(creationRequest.jdbcUser).isNotNull()
    }

    private fun Assert<RecordedRequest>.containsAuroraToken() = given { request ->
        request.headers[HttpHeaders.AUTHORIZATION]?.let {
            if (it.startsWith(HEADER_AURORA_TOKEN)) return
        }
        expected("Authorization header to contain $HEADER_AURORA_TOKEN")
    }
}