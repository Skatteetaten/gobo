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

        data class Param(val first: Int?, val after: String?)

        listOf(
            Param(first = null, after = null),
            Param(first = 20, after = null),
            Param(first = null, after = "some_cursor"),
            Param(first = 20, after = "some_cursor")
        ).forEach {
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
