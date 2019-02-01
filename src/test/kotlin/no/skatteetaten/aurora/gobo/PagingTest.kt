package no.skatteetaten.aurora.gobo

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import graphql.relay.DefaultEdge
import no.skatteetaten.aurora.gobo.resolvers.Cursor
import no.skatteetaten.aurora.gobo.resolvers.PagedEdges
import no.skatteetaten.aurora.gobo.resolvers.pageEdges
import org.junit.jupiter.api.Test

class PagingTest {

    @Test
    fun `paging with an empty list`() {

        data class Param(val first: Int?, val after: String?)

        listOf(
            Param(first = null, after = null),
            Param(first = 20, after = null),
            Param(first = null, after = "some_cursor"),
            Param(first = 20, after = "some_cursor")
        ).forEach {
            val pagedEdges = pageEdges(emptyList(), it.first, it.after)

            assertThat(pagedEdges.edges).isEmpty()
            assertThat(pagedEdges.totalCount).isEqualTo(0)

            val pageInfo = pagedEdges.pageInfo
            assertThat(pageInfo.startCursor).isNull()
            assertThat(pageInfo.endCursor).isNull()
            assertThat(pageInfo.isHasNextPage).isFalse()
            assertThat(pageInfo.isHasPreviousPage).isFalse()
        }
    }

    @Test
    fun `no paging`() = verify(
        pageEdges(edges),
        expectedEdges = toEdges("A", "B", "C", "D", "E"),
        hasPrevPage = false,
        hasNexPage = false
    )

    @Test
    fun `paging without cursor`() = verify(
        pageEdges(edges, first = 2),
        expectedEdges = toEdges("A", "B"),
        hasPrevPage = false,
        hasNexPage = true
    )

    @Test
    fun `paging to last page`() = verify(
        pageEdges(edges, first = 2, after = cursorOf("C")),
        expectedEdges = toEdges("D", "E"),
        hasPrevPage = true,
        hasNexPage = false
    )

    @Test
    fun `paging in middle of result set`() = verify(
        pageEdges(edges, first = 3, after = cursorOf("A")),
        expectedEdges = toEdges("B", "C", "D"),
        hasPrevPage = true,
        hasNexPage = true
    )

    data class Edge(private val node: String) : DefaultEdge<String>(node, Cursor(node))

    companion object {
        val edges = toEdges("A", "B", "C", "D", "E")
        fun toEdges(vararg names: String) = names.map { Edge(it) }
        fun cursorOf(s: String): String = Cursor(s).value
        fun verify(
            pageEdges: PagedEdges<Edge>,
            expectedEdges: List<Edge>,
            hasPrevPage: Boolean,
            hasNexPage: Boolean
        ) {

            val (edges, pageInfo, totalCount) = pageEdges

            assertThat(totalCount).isEqualTo(Companion.edges.size)

            assertThat(edges.size).isEqualTo(expectedEdges.size)
            assertThat(edges).isEqualTo(expectedEdges)
            assertThat(pageInfo.isHasPreviousPage).isEqualTo(hasPrevPage)
            assertThat(pageInfo.isHasNextPage).isEqualTo(hasNexPage)
            assertThat(pageInfo.startCursor.value).isEqualTo(expectedEdges.first().cursor.value)
            assertThat(pageInfo.endCursor.value).isEqualTo(expectedEdges.last().cursor.value)
        }
    }
}
