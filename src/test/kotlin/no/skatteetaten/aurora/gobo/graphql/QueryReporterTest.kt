package no.skatteetaten.aurora.gobo.graphql

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.Duration

class QueryReporterTest {

    @Test
    fun `Test add and remove`() {
        val reporter = QueryReporter(reportAfterMillis = 0)
        reporter.add("test123", "junit-test", "korrid1", "klientid", "getAffiliations", "getAffiliations {}")
        val afterAdd = reporter.awaitUnfinishedQueries()
        assertThat(afterAdd).hasSize(1)

        reporter.remove("test123")
        val afterRemove = reporter.awaitEmptyQueries()
        assertThat(afterRemove).isEmpty()
    }

    private fun QueryReporter.awaitUnfinishedQueries() = await()
        .atMost(Duration.ofSeconds(1))
        .until { unfinishedQueries().isNotEmpty() }
        .let { unfinishedQueries() }

    private fun QueryReporter.awaitEmptyQueries() = await()
        .atMost(Duration.ofSeconds(1))
        .until { queries().isEmpty() }
        .let { queries() }
}
