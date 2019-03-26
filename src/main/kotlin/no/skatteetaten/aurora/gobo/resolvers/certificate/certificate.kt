package no.skatteetaten.aurora.gobo.resolvers.certificate

import no.skatteetaten.aurora.gobo.resolvers.GoboConnection
import no.skatteetaten.aurora.gobo.resolvers.GoboEdge
import no.skatteetaten.aurora.gobo.resolvers.GoboPageInfo
import java.time.Instant

data class Certificate(
    val id: String,
    val cn: String,
    val issuedDate: Instant?,
    val revokedDate: Instant?,
    val expiresDate: Instant?
)

data class CertificateEdge(val node: Certificate) : GoboEdge(node.id)

data class CertificatesConnection(override val edges: List<CertificateEdge>, override val pageInfo: GoboPageInfo?) :
    GoboConnection<CertificateEdge>()
