package no.skatteetaten.aurora.gobo.graphql.certificate

import no.skatteetaten.aurora.gobo.graphql.GoboPageInfo
import no.skatteetaten.aurora.gobo.graphql.application.Certificate
import org.springframework.util.Base64Utils

data class CertificateEdge(val node: Certificate) {
    val cursor: String
        get() = Base64Utils.encodeToString(node.id.toByteArray())
}

data class CertificatesConnection(val edges: List<CertificateEdge>, val pageInfo: GoboPageInfo?, val totalCount: Int = edges.size)
