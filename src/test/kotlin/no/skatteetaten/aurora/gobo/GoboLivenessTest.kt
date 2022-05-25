package no.skatteetaten.aurora.gobo

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.graphql.QueryReporter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GoboLivenessTest {
    private val queryReporter = QueryReporter(reportAfterMillis = 0)
    private val liveness = GoboLiveness(
        queryReporter = queryReporter,
        maxUnfinishedQueries = 2,
        goboMetrics = mockk()
    )

    @BeforeEach
    internal fun setUp() {
        queryReporter.clear()
    }

    @Test
    fun `Get unfinished queries`() {
        queryReporter.add("test123", "klientId", "name", "query")
        val unfinished = liveness.unfinishedQueries()

        assertThat(unfinished.success).isTrue()
        assertThat(unfinished.queries).hasSize(1)
        assertThat(unfinished.queries.first().korrelasjonsid).isEqualTo("test123")
    }

    @Test
    fun `Unfinished queries check fails`() {
        queryReporter.add("test123", "klientId", "name", "query")
        queryReporter.add("test456", "klientId", "name", "query")
        queryReporter.add("test789", "klientId", "name", "query")

        val unfinished = liveness.unfinishedQueries()
        assertThat(unfinished.success).isFalse()
        assertThat(unfinished.queries).hasSize(3)
    }
}
