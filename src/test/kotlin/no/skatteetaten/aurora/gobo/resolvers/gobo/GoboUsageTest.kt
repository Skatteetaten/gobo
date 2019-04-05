package no.skatteetaten.aurora.gobo.resolvers.gobo

import assertk.assertThat
import assertk.assertions.hasSize
import org.junit.jupiter.api.Test

class GoboUsageTest {
    private val usage = GoboUsage(listOf(GoboField("abc", 1), GoboField("bcd", 1)))

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