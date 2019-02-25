package no.skatteetaten.aurora.gobo.integration.dbh

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresDbh
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RequiresDbh::class, DatabaseSchemaServiceBlocking::class, DatabaseSchemaServiceDisabled::class],
    properties = ["integrations.dbh.url=false"]
)
class DisableDatabaseSchemaServiceTest {

    @Autowired(required = false)
    private var databaseSchemaServiceBlocking: DatabaseSchemaServiceBlocking? = null

    @Autowired(required = false)
    private var databaseSchemaServiceDisabled: DatabaseSchemaServiceDisabled? = null

    @Test
    fun `Disable DBH given no url configured`() {
        assertThat(databaseSchemaServiceBlocking).isNull()
        assertThat(databaseSchemaServiceDisabled).isNotNull()
    }
}