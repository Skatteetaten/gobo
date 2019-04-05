package no.skatteetaten.aurora.gobo.resolvers.certificate

import no.skatteetaten.aurora.gobo.integration.skap.Certificate
import no.skatteetaten.aurora.gobo.resolvers.GoboConnection
import no.skatteetaten.aurora.gobo.resolvers.GoboEdge
import no.skatteetaten.aurora.gobo.resolvers.GoboPageInfo

data class CertificateEdge(val node: Certificate) : GoboEdge(node.id)

data class CertificatesConnection(override val edges: List<CertificateEdge>, override val pageInfo: GoboPageInfo?) :
    GoboConnection<CertificateEdge>()
