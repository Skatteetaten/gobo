package no.skatteetaten.aurora.gobo.resolvers

import java.lang.Math.min

data class GoboPagedEdges<T>(val edges: List<T>, val pageInfo: GoboPageInfo, val totalCount: Int)

fun <T : GoboEdge> pageEdges(allEdges: List<T>, first: Int? = null, after: String? = null): GoboPagedEdges<T> {

    val edges = createPage(allEdges, first, after)
    val pageInfo = createPageInfo(edges, allEdges)
    return GoboPagedEdges(edges, pageInfo, allEdges.size)
}

private fun <T : GoboEdge> createPage(edges: List<T>, first: Int?, afterCursor: String?): List<T> {
    val startIndex = if (afterCursor != null) edges.indexOfFirst { it.cursor.value == afterCursor } + 1 else 0
    return createPage(edges, startIndex, first ?: edges.size)
}

fun <T> createPage(edges: List<T>, offset: Int, limit: Int = edges.size): List<T> {
    val endIndex = min(offset + limit, edges.size)
    return edges.subList(offset, endIndex)
}

fun <T : GoboEdge> createPageInfo(pageEdges: List<T>, allEdges: List<T> = pageEdges): GoboPageInfo {

    data class Cursors<T : GoboEdge>(private val edges: List<T>) {
        val first get() = edges.firstOrNull()?.cursor
        val last get() = edges.lastOrNull()?.cursor

        fun isAtStartOf(cursors: Cursors<T>) = cursors.first != null && cursors.first?.value != this.first?.value
        fun isAtEndOf(cursors: Cursors<T>) = cursors.last != null && cursors.last?.value != this.last?.value
    }

    val page = Cursors(pageEdges)
    val all = Cursors(allEdges)
    return GoboPageInfo(page.first, page.last, page.isAtStartOf(all), page.isAtEndOf(all))
}
