package no.skatteetaten.aurora.gobo.integration.dbh

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.RequiresDbh
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [RequiresDbh::class, DatabaseServiceBlocking::class, DatabaseServiceDisabled::class],
    properties = ["integrations.dbh.url=false"]
)
class DisableDatabaseServiceTest {

    @Autowired(required = false)
    private var databaseServiceBlocking: DatabaseServiceBlocking? = null

    @Autowired(required = false)
    private var databaseServiceDisabled: DatabaseServiceDisabled? = null

    @Test
    fun `Disable DBH given no url configured`() {
        assertThat(databaseServiceBlocking).isNull()
        assertThat(databaseServiceDisabled).isNotNull()
    }
}
