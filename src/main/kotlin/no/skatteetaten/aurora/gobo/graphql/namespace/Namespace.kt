package no.skatteetaten.aurora.gobo.graphql.namespace

import no.skatteetaten.aurora.gobo.graphql.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.graphql.permission.Permission

data class Namespace(val name: String, val affiliation: Affiliation, val permission: Permission)
