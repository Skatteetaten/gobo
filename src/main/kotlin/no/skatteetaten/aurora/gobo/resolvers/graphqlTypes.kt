package no.skatteetaten.aurora.gobo.resolvers

import graphql.relay.DefaultConnectionCursor
import graphql.relay.PageInfo
import org.springframework.util.Base64Utils

abstract class Connection<T> {
    abstract val edges: List<T>
    abstract val pageInfo: PageInfo?

    open val totalCount: Int
        get() = edges.size
}

class Cursor(value: String) : DefaultConnectionCursor(Base64Utils.encodeToString(value.toByteArray()))