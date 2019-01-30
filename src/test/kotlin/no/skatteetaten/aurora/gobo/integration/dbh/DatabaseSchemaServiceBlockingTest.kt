package no.skatteetaten.aurora.gobo.integration.dbh

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.message
import assertk.catch
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.JsonPath
import io.mockk.every
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.SchemaCreationRequestBuilder
import no.skatteetaten.aurora.gobo.SchemaDeletionRequestBuilder
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.execute
import no.skatteetaten.aurora.gobo.security.SharedSecretReader
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient.create

class DatabaseSchemaServiceBlockingTest {
    private val server = MockWebServer()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "abc123"
    }
    private val databaseSchemaService =
        DatabaseSchemaServiceBlocking(DatabaseSchemaService(sharedSecretReader, create(server.url("/").toString())))

    @Test
    fun `Get database schemas given affiliation`() {
        val response = Response(items = listOf(DatabaseSchemaResourceBuilder().build()))
        val request = server.execute(response) {
            val databaseSchemas = databaseSchemaService.getDatabaseSchemas("paas")
            assert(databaseSchemas).hasSize(1)
        }
        assert(request.path).contains("affiliation")
        assert(request.path).contains("paas")
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
            assert(databaseSchemas).hasSize(1)
        }
        assert(request.path).contains("affiliation")
        assert(request.path).contains("paas")
    }

    @Test
    fun `Get database schemas return failed response`() {
        val response = Response<DatabaseSchemaResource>(message = "failed", success = false, items = emptyList())
        server.execute(response) {
            val exception = catch { databaseSchemaService.getDatabaseSchemas("paas") }
            assert(exception).isNotNull {
                it.isInstanceOf(SourceSystemException::class)
                it.message().isEqualTo("failed")
            }
        }
    }

    @Test
    fun `Get database schema given id`() {
        val response = Response(items = listOf(DatabaseSchemaResourceBuilder().build()))
        val request = server.execute(response) {
            val databaseSchema = databaseSchemaService.getDatabaseSchema("abc123")
            assert(databaseSchema).isNotNull()
        }
        assert(request.path).endsWith("/abc123")
    }

    @Test
    fun `Update database schema`() {
        val response = Response(items = listOf(DatabaseSchemaResourceBuilder().build()))
        val request = server.execute(response) {
            val databaseSchema =
                databaseSchemaService.updateDatabaseSchema(SchemaCreationRequestBuilder("123").build())
            assert(databaseSchema).isNotNull()
        }
        assert(request.path).endsWith("/123")
    }

    @Test
    fun `Delete database schema without cooldownDurationHours`() {
        val response = Response(items = emptyList<DatabaseSchemaResource>())
        val request = server.execute(response) {
            val deleted = databaseSchemaService.deleteDatabaseSchema(SchemaDeletionRequestBuilder(id = "123").build())
            assert(deleted).isTrue()
        }
        assert(request.path).endsWith("/123")
        assert(request.headers[HEADER_COOLDOWN_DURATION_HOURS]).isNull()
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
            assert(deleted).isTrue()
        }
        assert(request.path).endsWith("/123")
        assert(request.headers[HEADER_COOLDOWN_DURATION_HOURS]).isEqualTo("2")
    }
}