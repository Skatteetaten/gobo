package no.skatteetaten.aurora.gobo.integration.dbh

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.DatabaseSchemaResourceBuilder
import org.junit.jupiter.api.Test
import java.time.Instant

class DatabaseSchemaResourceTest {

    private val now = Instant.now()
    private val databaseSchemaResource =
        DatabaseSchemaResourceBuilder(createdDate = now.toEpochMilli(), lastUsedDate = now.toEpochMilli()).build()

    @Test
    fun `Get properties from labels`() {
        assertThat(databaseSchemaResource.affiliation).isEqualTo("aurora")
        assertThat(databaseSchemaResource.createdBy).isEqualTo("abc123")
        assertThat(databaseSchemaResource.discriminator).isEqualTo("referanse")
        assertThat(databaseSchemaResource.description).isEqualTo("my database schema")
    }

    @Test
    fun `Get milliseconds as Instant`() {
        val created = databaseSchemaResource.createdDateAsInstant()
        val lastUsed = databaseSchemaResource.lastUsedDateAsInstant()
        assertThat(created).isEqualTo(now)
        assertThat(lastUsed).isEqualTo(now)
    }

    @Test
    fun `Get lastUsed with null value`() {
        val db = DatabaseSchemaResourceBuilder(lastUsedDate = null).build()
        val lastUsed = db.lastUsedDateAsInstant()
        assertThat(lastUsed).isNull()
    }
}