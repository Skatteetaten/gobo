package no.skatteetaten.aurora.gobo.graphql.certificate

import no.skatteetaten.aurora.gobo.graphql.GoboPageInfo
import no.skatteetaten.aurora.gobo.graphql.application.Certificate

data class CertificateEdge(val node: Certificate)

data class CertificatesConnection(val edges: List<CertificateEdge>, val pageInfo: GoboPageInfo?, val totalCount: Int = edges.size)
