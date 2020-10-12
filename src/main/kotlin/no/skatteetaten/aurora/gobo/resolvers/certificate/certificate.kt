package no.skatteetaten.aurora.gobo.resolvers.certificate

import no.skatteetaten.aurora.gobo.resolvers.GoboPageInfo
import no.skatteetaten.aurora.gobo.resolvers.application.Certificate

data class CertificateEdge(val node: Certificate)

data class CertificatesConnection(val edges: List<CertificateEdge>, val pageInfo: GoboPageInfo?, val totalCount: Int = edges.size)
