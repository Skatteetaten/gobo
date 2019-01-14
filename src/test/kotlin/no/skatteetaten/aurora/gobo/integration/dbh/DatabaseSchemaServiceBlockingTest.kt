package no.skatteetaten.aurora.gobo.integration.dbh

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.message
import assertk.catch
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.SourceSystemException
import no.skatteetaten.aurora.gobo.integration.execute
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient.create

class DatabaseSchemaServiceBlockingTest {
    private val server = MockWebServer()
    private val databaseSchemaService =
        DatabaseSchemaServiceBlocking(DatabaseSchemaService("abc123", create(server.url("/").toString())))

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
            assert(databaseSchema).hasSize(1)
        }
        assert(request.path).endsWith("/abc123")
    }
}