package no.skatteetaten.aurora.gobo.resolvers

import graphql.relay.DefaultEdge
import graphql.relay.DefaultPageInfo
import graphql.relay.PageInfo

data class PagedEdges<T>(val edges: List<T>, val pageInfo: PageInfo, val totalCount: Int)

fun <T : DefaultEdge<*>> pageEdges(allEdges: List<T>, first: Int?, after: String?): PagedEdges<T> {

    val edges = createPage(allEdges, first, after)
    val pageInfo = createPageInfo(edges, allEdges)
    return PagedEdges(edges, pageInfo, allEdges.size)
}

private fun <T : DefaultEdge<*>> createPage(allEdges: List<T>, first: Int?, after: String?): List<T> {

    var edges = allEdges

    if (after != null) {
        val startIndex = edges.indexOfFirst { it.cursor.value == after }
        if (startIndex != -1) {
            edges = edges.subList(startIndex + 1, edges.lastIndex + 1)
        }
    }
    if (first != null) {
        edges = edges.take(first)
    }
    return edges
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