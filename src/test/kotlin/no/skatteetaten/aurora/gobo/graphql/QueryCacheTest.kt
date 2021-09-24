package no.skatteetaten.aurora.gobo.graphql

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.actuate.info.Info

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class QueryCacheTest {

    private val queryCache = QueryCache()

    @AfterEach
    fun setUp() {
        queryCache.clear()
    }

    @Test
    fun `Cache query`() {
        queryCache.add(klientid = "klient", korrelasjonsid = "abc123", query = "my graphql query")
        val queries = queryCache.get()

        assertThat(queries).hasSize(1)
    }

    @Test
    fun `Add query data to actuator info`() {
        val infoBuilder = Info.Builder()

        queryCache.add(klientid = "klient", korrelasjonsid = "abc123", query = "my graphql query")
        queryCache.contribute(infoBuilder)

        @Suppress("UNCHECKED_CAST")
        val queries = infoBuilder.build().get("queries") as List<QueryEntry>
        assertThat(queries).hasSize(1)
        assertThat(queries.first().korrelasjonsid).isEqualTo("abc123")
    }
}
