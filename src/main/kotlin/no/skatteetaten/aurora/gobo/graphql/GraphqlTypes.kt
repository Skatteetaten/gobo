package no.skatteetaten.aurora.gobo.graphql

import org.springframework.util.Base64Utils

class GoboCursor(input: String) {
    val value: String = Base64Utils.encodeToString(input.toByteArray())

    override fun toString() = value
}

abstract class GoboEdge(name: String) {
    val cursor = GoboCursor(name).value
}

data class GoboPageInfo(
    val startCursor: String?,
    val endCursor: String?,
    val hasPreviousPage: Boolean,
    val hasNextPage: Boolean
)

abstract class GoboItems(private val items: List<Any>) {
    fun totalCount() = items.size
}
