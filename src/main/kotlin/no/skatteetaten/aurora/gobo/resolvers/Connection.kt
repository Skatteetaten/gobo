package no.skatteetaten.aurora.gobo.resolvers

import graphql.relay.PageInfo

abstract class Connection<T> {
    abstract val edges: List<T>
    abstract val pageInfo: PageInfo?

    fun totalCount() = edges.size
}