package no.skatteetaten.aurora.gobo.resolvers.certificate

/*
@Component
class CertificateResolver(private val certificateService: CertificateService) : GraphQLQueryResolver {

    fun getCertificates(dfe: DataFetchingEnvironment): CertificatesConnection {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get certificates")
        val certificates = certificateService.getCertificates().map { CertificateEdge(it) }
        return CertificatesConnection(edges = certificates, pageInfo = null)
    }
}
*/
