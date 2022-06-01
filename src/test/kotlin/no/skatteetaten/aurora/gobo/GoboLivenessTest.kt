package no.skatteetaten.aurora.gobo

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.mockk.mockk
import no.skatteetaten.aurora.gobo.graphql.QueryReporter
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

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
        queryReporter.add("test123", "trace1", "korrid1", "klientId", "name", "query")

        val unfinished = liveness.awaitUnfinishedQueries()
        assertThat(unfinished.success).isTrue()
        assertThat(unfinished.queries).hasSize(1)
        assertThat(unfinished.queries.first().korrelasjonsid).isEqualTo("korrid1")
    }

    @Test
    fun `Unfinished queries check fails`() {
        queryReporter.add("test234", "trace1", "korrid1", "klientId", "name", "query")
        queryReporter.add("test567", "trace2", "korrid2", "klientId", "name", "query")
        queryReporter.add("test890", "trace3", "korrid3", "klientId", "name", "query")

        val unfinished = liveness.awaitUnfinishedQueries()
        assertThat(unfinished.success).isFalse()
        assertThat(unfinished.queries).hasSize(3)
    }

    private fun GoboLiveness.awaitUnfinishedQueries() = await()
        .atMost(Duration.ofSeconds(1))
        .until { unfinishedQueries().queries.isNotEmpty() }
        .let { unfinishedQueries() }
}
