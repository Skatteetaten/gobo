package no.skatteetaten.aurora.gobo.resolvers

data class PageInfo(val startCursor: String?, val endCursor: String?, val hasPreviousPage: Boolean, val hasNextPage: Boolean?)