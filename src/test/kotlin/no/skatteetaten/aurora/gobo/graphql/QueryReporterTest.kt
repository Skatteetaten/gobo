package no.skatteetaten.aurora.gobo.graphql

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test

class QueryReporterTest {
    @Test
    fun `Test add and report`() {
        val reporter = QueryReporter(reportAfterMillis = 0)
        reporter.add("test123", "junit-test", "getAffiliations", "getAffiliations {}")
        val unfinished = reporter.reportUnfinished()
        assertThat(unfinished).hasSize(1)
    }

    @Test
    fun `Test add and report before timeout`() {
        val reporter = QueryReporter(reportAfterMillis = 5000)
        reporter.add("test123", "junit-test", "getAffiliations", "getAffiliations {}")
        val unfinished = reporter.reportUnfinished()
        assertThat(unfinished).isEmpty()
    }

    @Test
    fun `Test add and remove`() {
        val reporter = QueryReporter(reportAfterMillis = 5000)
        reporter.add("test123", "junit-test", "getAffiliations", "getAffiliations {}")
        reporter.remove("test123")
        val unfinished = reporter.reportUnfinished()
        assertThat(unfinished).isEmpty()
    }
}
