package no.skatteetaten.aurora.gobo.resolvers

import graphql.relay.DefaultEdge
import graphql.relay.DefaultPageInfo
import graphql.relay.PageInfo
import java.lang.Math.min

data class PagedEdges<T>(val edges: List<T>, val pageInfo: PageInfo, val totalCount: Int)

fun <T : DefaultEdge<*>> pageEdges(allEdges: List<T>, first: Int? = null, after: String? = null): PagedEdges<T> {

    val edges = createPage(allEdges, first, after)
    val pageInfo = createPageInfo(edges, allEdges)
    return PagedEdges(edges, pageInfo, allEdges.size)
}

private fun <T : DefaultEdge<*>> createPage(edges: List<T>, first: Int?, after: String?): List<T> {

    val startIndex = if (after != null) edges.indexOfFirst { it.cursor.value == after } + 1 else 0
    val endIndex = if (first != null) min(startIndex + first, edges.size) else edges.size

    return edges.subList(startIndex, endIndex)
}

private fun <T : DefaultEdge<*>> createPageInfo(pageEdges: List<T>, allEdges: List<T>): DefaultPageInfo {

    data class Cursors<T : DefaultEdge<*>>(private val edges: List<T>) {
        val first get() = edges.firstOrNull()?.cursor
        val last get() = edges.lastOrNull()?.cursor

        fun isAtStartOf(cursors: Cursors<T>) = cursors.first != null && cursors.first?.value != this.first?.value
        fun isAtEndOf(cursors: Cursors<T>) = cursors.last != null && cursors.last?.value != this.last?.value
    }

    val page = Cursors(pageEdges)
    val all = Cursors(allEdges)
    return DefaultPageInfo(page.first, page.last, page.isAtStartOf(all), page.isAtEndOf(all))
}