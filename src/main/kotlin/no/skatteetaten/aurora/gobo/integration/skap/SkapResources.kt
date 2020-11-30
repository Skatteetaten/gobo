package no.skatteetaten.aurora.gobo.integration.skap

import no.skatteetaten.aurora.gobo.graphql.webseal.Acl

const val HEADER_AURORA_TOKEN = "aurora-token"

data class WebsealStateResource(
    val acl: Acl,
    val junctions: List<Map<String, String>>,
    val name: String,
    val namespace: String,
    val routeName: String
)

data class SkapJob(
    val id: String,
    val payload: String,
    val type: String,
    val operation: String,
    val status: String,
    val updated: String,
    val errorMessage: String? = null
)
