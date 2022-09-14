package no.skatteetaten.aurora.gobo.graphql

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.time.Duration

class QueryReporterTest {
    private val reporter = QueryReporter(reportAfterMinutes = 0, unfinishedQueriesExpireMinutes = 1)

    @Test
    fun `Test add and remove`() {
        reporter.add("test123", "junit-test", "korrid1", "klientid", "getAffiliations", "getAffiliations {}")
        val afterAdd = reporter.awaitUnfinishedQueries()
        assertThat(afterAdd).hasSize(1)

        reporter.remove("test123")
        val afterRemove = reporter.awaitEmptyQueries()
        assertThat(afterRemove).isEmpty()
    }

    @Test
    fun `Trying to remove id not in cache`() {
        reporter.remove("123")
        assertThat(reporter.queries()).isEmpty()
    }

    @Test
    @EnabledOnOs(OS.MAC, OS.LINUX)
    fun `Get number of open file descriptors`() {
        val fileDesc = reporter.numberOfOpenFileDescriptors()
        assertThat(fileDesc).isNotNull().isGreaterThan(0)
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
