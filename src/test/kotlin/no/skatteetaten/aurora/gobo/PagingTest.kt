package no.skatteetaten.aurora.gobo

import assertk.assert
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

        data class P(val first: Int?, val after: String?)

        listOf(P(null, null), P(20, null), P(null, "df"), P(20, "df")).forEach {
            val pagedEdges = pageEdges(emptyList(), it.first, it.after)

            assert(pagedEdges.edges).isEmpty()
            assert(pagedEdges.totalCount).isEqualTo(0)

            val pageInfo = pagedEdges.pageInfo
            assert(pageInfo.startCursor).isNull()
            assert(pageInfo.endCursor).isNull()
            assert(pageInfo.isHasNextPage).isFalse()
            assert(pageInfo.isHasPreviousPage).isFalse()
        }
    }

    @Test
    fun `no paging`() = verify(pageEdges(edges), toEdges("A", "B", "C", "D", "E"), false, false)

    @Test
    fun `paging without cursor`() = verify(pageEdges(edges, 2), toEdges("A", "B"), false, true)

    @Test
    fun `paging to last page`() = verify(pageEdges(edges, 2, cursorOf("C")), toEdges("D", "E"), true, false)

    @Test
    fun `paging in middle of result set`() =
        verify(pageEdges(edges, 3, cursorOf("A")), toEdges("B", "C", "D"), true, true)

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

            assert(totalCount).isEqualTo(Companion.edges.size)

            assert(edges.size).isEqualTo(expectedEdges.size)
            assert(edges).isEqualTo(expectedEdges)
            assert(pageInfo.isHasPreviousPage).isEqualTo(hasPrevPage)
            assert(pageInfo.isHasNextPage).isEqualTo(hasNexPage)
            assert(pageInfo.startCursor.value).isEqualTo(expectedEdges.first().cursor.value)
            assert(pageInfo.endCursor.value).isEqualTo(expectedEdges.last().cursor.value)
        }
    }
}
