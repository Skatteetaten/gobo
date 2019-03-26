package no.skatteetaten.aurora.gobo.resolvers.certificate

import com.coxautodev.graphql.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.resolvers.AccessDeniedException
import no.skatteetaten.aurora.gobo.security.isAnonymousUser
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CertificateResolver : GraphQLQueryResolver {

    fun getCertificates(dfe: DataFetchingEnvironment): CertificatesConnection {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get certificates")
        return CertificatesConnection(
            edges = listOf(
                CertificateEdge(Certificate("123", "Test1", Instant.now(), Instant.now(), Instant.now())),
                CertificateEdge(Certificate("234", "Test2", Instant.now(), Instant.now(), Instant.now()))
            ),
            pageInfo = null
        )
    }
}