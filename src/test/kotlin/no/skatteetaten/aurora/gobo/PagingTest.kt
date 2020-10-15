package no.skatteetaten.aurora.gobo

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import no.skatteetaten.aurora.gobo.graphql.GoboCursor
import no.skatteetaten.aurora.gobo.graphql.GoboEdge
import no.skatteetaten.aurora.gobo.graphql.GoboPagedEdges
import no.skatteetaten.aurora.gobo.graphql.pageEdges
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
            assertThat(pageInfo.hasNextPage).isFalse()
            assertThat(pageInfo.hasPreviousPage).isFalse()
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

    data class Edge(val node: String) : GoboEdge(node)

    companion object {
        val edges = toEdges("A", "B", "C", "D", "E")
        fun toEdges(vararg names: String) = names.map { Edge(it) }
        fun cursorOf(s: String): String = GoboCursor(s).value
        fun verify(
            pageEdges: GoboPagedEdges<Edge>,
            expectedEdges: List<Edge>,
            hasPrevPage: Boolean,
            hasNexPage: Boolean
        ) {

            val (edges, pageInfo, totalCount) = pageEdges

            assertThat(totalCount).isEqualTo(Companion.edges.size)

            assertThat(edges.size).isEqualTo(expectedEdges.size)
            assertThat(edges).isEqualTo(expectedEdges)
            assertThat(pageInfo.hasPreviousPage).isEqualTo(hasPrevPage)
            assertThat(pageInfo.hasNextPage).isEqualTo(hasNexPage)
            assertThat(pageInfo.startCursor).isEqualTo(expectedEdges.first().cursor.value)
            assertThat(pageInfo.endCursor).isEqualTo(expectedEdges.last().cursor.value)
        }
    }
}
