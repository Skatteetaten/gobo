package no.skatteetaten.aurora.gobo.resolvers.namespace

import no.skatteetaten.aurora.gobo.resolvers.affiliation.Affiliation
import no.skatteetaten.aurora.gobo.resolvers.permission.Permission

data class Namespace(val name: String, val affiliation: Affiliation, val permission: Permission)
