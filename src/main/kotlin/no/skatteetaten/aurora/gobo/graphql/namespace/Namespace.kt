package no.skatteetaten.aurora.gobo.graphql.namespace

import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.graphql.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.graphql.loadValue
import no.skatteetaten.aurora.gobo.graphql.permission.Permission

data class Namespace(val name: String, val affiliation: Affiliation) {
    fun permission(dfe: DataFetchingEnvironment) = dfe.loadValue<Namespace, Permission>(this)
}
