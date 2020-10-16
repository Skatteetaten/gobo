package no.skatteetaten.aurora.gobo.graphql

import org.springframework.util.Base64Utils

abstract class GoboConnection<T> {
    abstract val edges: List<T>
    abstract val pageInfo: GoboPageInfo?

    open val totalCount: Int
        get() = edges.size
}

class GoboCursor(input: String) {
    val value: String = Base64Utils.encodeToString(input.toByteArray())

    override fun toString() = value
}

abstract class GoboEdge(name: String) {
    val cursor = GoboCursor(name)
}

data class GoboPageInfo(
    val startCursor: String?,
    val endCursor: String?,
    val hasPreviousPage: Boolean,
    val hasNextPage: Boolean
)