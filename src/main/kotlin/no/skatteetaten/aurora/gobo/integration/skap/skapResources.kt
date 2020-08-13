package no.skatteetaten.aurora.gobo.integration.skap

import no.skatteetaten.aurora.gobo.resolvers.webseal.Acl
import java.time.Instant

data class Certificate(
    val id: String,
    val dn: String,
    val issuedDate: Instant?,
    val revokedDate: Instant?,
    val expiresDate: Instant?
)

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
