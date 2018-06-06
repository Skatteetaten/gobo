package no.skatteetaten.aurora.gobo.resolvers

abstract class Edge<T> {
    abstract val node: T
    abstract fun cursor(): String?
}