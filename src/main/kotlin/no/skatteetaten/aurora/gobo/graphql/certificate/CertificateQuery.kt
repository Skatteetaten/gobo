package no.skatteetaten.aurora.gobo.graphql.certificate

import com.expediagroup.graphql.spring.operations.Query
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.skap.CertificateService
import no.skatteetaten.aurora.gobo.security.checkValidUserToken
import org.springframework.stereotype.Component

@Component
class CertificateQuery(private val certificateService: CertificateService) : Query {

    suspend fun certificates(dfe: DataFetchingEnvironment): CertificatesConnection {
        dfe.checkValidUserToken()
        val certificates = certificateService.getCertificates().map { CertificateEdge(it) }
        return CertificatesConnection(edges = certificates, pageInfo = null)
    }
}
