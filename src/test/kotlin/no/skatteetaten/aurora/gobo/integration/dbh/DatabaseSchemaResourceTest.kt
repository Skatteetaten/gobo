package no.skatteetaten.aurora.gobo.integration.dbh

import assertk.assert
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
        assert(databaseSchemaResource.affiliation).isEqualTo("aurora")
        assert(databaseSchemaResource.createdBy).isEqualTo("abc123")
        assert(databaseSchemaResource.appDbName).isEqualTo("referanse")
        assert(databaseSchemaResource.description).isEqualTo("my database schema")
    }

    @Test
    fun `Get milliseconds as Instant`() {
        val created = databaseSchemaResource.createdDateAsInstant()
        val lastUsed = databaseSchemaResource.lastUsedDateAsInstant()
        assert(created).isEqualTo(now)
        assert(lastUsed).isEqualTo(now)
    }

    @Test
    fun `Get lastUsed with null value`() {
        val db = DatabaseSchemaResourceBuilder(lastUsedDate = null).build()
        val lastUsed = db.lastUsedDateAsInstant()
        assert(lastUsed).isNull()
    }
}