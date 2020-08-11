package no.skatteetaten.aurora.gobo.resolvers.database

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.integration.dbh.SchemaCooldownChangeResponse
import org.junit.jupiter.api.Test

class DeleteDatabaseSchemasResponseTest {

    @Test
    fun `Create DeleteDatabaseSchemas with succeeded and failed id`() {
        val responses = listOf(
            SchemaCooldownChangeResponse(id = "123", success = true),
            SchemaCooldownChangeResponse(id = "234", success = false)
        )
        val deleteDatabaseSchemasResponse = CooldownChangeDatabaseSchemasResponse.create(responses)
        assertThat(deleteDatabaseSchemasResponse.succeeded.size).isEqualTo(1)
        assertThat(deleteDatabaseSchemasResponse.succeeded.first()).isEqualTo("123")
        assertThat(deleteDatabaseSchemasResponse.failed.size).isEqualTo(1)
        assertThat(deleteDatabaseSchemasResponse.failed.first()).isEqualTo("234")
    }
}
