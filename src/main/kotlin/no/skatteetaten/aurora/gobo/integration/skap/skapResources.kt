package no.skatteetaten.aurora.gobo.integration.skap

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

data class Routes(
    val progressions: List<Progression>
)

data class Progression(
    val id: Int,
    val payload: String,
    val objectname: String,
    val namespace: String,
    val type: String,
    val operation: String,
    val status: String,
    val updated: String,
    val errorMessage: String?
)

data class Acl(
    val aclName: String,
    val anyOther: Boolean,
    val `open`: Boolean,
    val roles: List<String>
)
