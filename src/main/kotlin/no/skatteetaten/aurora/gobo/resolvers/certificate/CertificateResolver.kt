package no.skatteetaten.aurora.gobo.resolvers.certificate

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.integration.skap.CertificateService
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component

@Component
class CertificateResolver(private val certificateService: CertificateService) : GraphQLQueryResolver {

    fun getCertificates(dfe: DataFetchingEnvironment): CertificatesConnection {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get certificates")
        val certificates = certificateService.getCertificates().map { CertificateEdge(it) }
        return CertificatesConnection(edges = certificates, pageInfo = null)
    }
}