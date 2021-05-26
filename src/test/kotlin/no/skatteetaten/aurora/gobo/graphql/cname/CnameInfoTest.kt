package no.skatteetaten.aurora.gobo.graphql.cname

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import no.skatteetaten.aurora.gobo.CnameInfoBuilder
import org.junit.jupiter.api.Test

class CnameInfoTest {

    private val cnameInfo = CnameInfoBuilder().build()

    @Test
    fun `Contains affiliation`() {
        assertThat(cnameInfo.containsAffiliation(listOf("aurora"))).isTrue()
    }

    @Test
    fun `Does not contain affiliation`() {
        assertThat(cnameInfo.containsAffiliation(listOf("demo", "paas"))).isFalse()
    }
}
