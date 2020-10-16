package no.skatteetaten.aurora.gobo.graphql.certificate

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.skap.CertificateService
import org.springframework.stereotype.Component

@Component
class CertificateQuery(private val certificateService: CertificateService) : Query {

    // FIXME no anonymous access
    suspend fun certificates(dfe: DataFetchingEnvironment): CertificatesConnection {
        val certificates = certificateService.getCertificates().map { CertificateEdge(it) }
        return CertificatesConnection(edges = certificates, pageInfo = null)
    }
}
