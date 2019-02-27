package no.skatteetaten.aurora.gobo.resolvers.databaseschema

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaDeletionResponse
import org.junit.jupiter.api.Test

class DeleteDatabaseSchemasResponseTest {

    @Test
    fun `Create DeleteDatabaseSchemas with succeeded and failed id`() {
        val responses = listOf(
            SchemaDeletionResponse(id = "123", success = true),
            SchemaDeletionResponse(id = "234", success = false)
        )
        val deleteDatabaseSchemasResponse = DeleteDatabaseSchemasResponse.create(responses)
        assertThat(deleteDatabaseSchemasResponse.succeeded.size).isEqualTo(1)
        assertThat(deleteDatabaseSchemasResponse.succeeded.first()).isEqualTo("123")
        assertThat(deleteDatabaseSchemasResponse.failed.size).isEqualTo(1)
        assertThat(deleteDatabaseSchemasResponse.failed.first()).isEqualTo("234")
    }
}