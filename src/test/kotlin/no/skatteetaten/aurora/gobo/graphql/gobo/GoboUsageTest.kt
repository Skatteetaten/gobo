package no.skatteetaten.aurora.gobo.graphql.gobo

import assertk.assertThat
import assertk.assertions.hasSize
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.LongAdder

class GoboUsageTest {
    private val clients = listOf(GoboUser("donald", 12), GoboUser("dolly", 15))
    private val usage = GoboUsage(listOf(GoboFieldUsage("abc", LongAdder().apply { 1 }, clients), GoboFieldUsage("bcd", LongAdder().apply { 3 }, clients)), emptyList())

    @Test
    fun `Get fields with name containing`() {
        assertThat(usage.usedFields("a")).hasSize(1)
        assertThat(usage.usedFields("b")).hasSize(2)
    }

    @Test
    fun `Get all fields`() {
        assertThat(usage.usedFields(null)).hasSize(2)
    }
}
