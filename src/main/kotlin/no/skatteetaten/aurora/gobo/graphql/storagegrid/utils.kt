package no.skatteetaten.aurora.gobo.graphql.storagegrid

fun getTenantName(affiliation: String, cluster: String): String = "$affiliation-$cluster"
