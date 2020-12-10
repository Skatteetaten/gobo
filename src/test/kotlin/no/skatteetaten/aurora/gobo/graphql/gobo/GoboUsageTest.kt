package no.skatteetaten.aurora.gobo.graphql.gobo

import assertk.assertThat
import assertk.assertions.hasSize
import org.junit.jupiter.api.Test

class GoboUsageTest {
    private val clients = listOf(GoboClient("donald", 12), GoboClient("dolly", 15))
    private val usage = GoboUsage(listOf(GoboFieldUsage("abc", 1L, clients), GoboFieldUsage("bcd", 3L, clients)), emptyList())

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
