package no.skatteetaten.aurora.gobo

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gobo.graphql.QueryReporter
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.time.Duration

class GoboLivenessTest {
    private val queryReporter = QueryReporter(reportAfterMinutes = 0, unfinishedQueriesExpireMinutes = 1)
    private val liveness = GoboLiveness(
        queryReporter = queryReporter,
        maxUnfinishedQueries = 2,
        maxOpenFileDescriptors = 1
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

    @Test
    @EnabledOnOs(OS.MAC, OS.LINUX)
    fun `Get number of open file descriptors`() {
        val fileDesc = liveness.openFileDescriptors()
        assertThat(fileDesc.openFileDescriptors).isNotNull().isGreaterThan(0)
        assertThat(fileDesc.success).isFalse()
    }

    private fun GoboLiveness.awaitUnfinishedQueries() = await()
        .atMost(Duration.ofSeconds(1))
        .until { unfinishedQueries().queries.isNotEmpty() }
        .let { unfinishedQueries() }
}
