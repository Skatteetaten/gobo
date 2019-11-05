package no.skatteetaten.aurora.gobo.resolvers.certificate

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.CertificateResourceBuilder
import no.skatteetaten.aurora.gobo.integration.skap.CertificateService
import no.skatteetaten.aurora.gobo.resolvers.AbstractGraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class CertificateResolverTest : AbstractGraphQLTest() {

    @Value("classpath:graphql/queries/getCertificates.graphql")
    private lateinit var getCertificates: Resource

    @MockkBean
    private lateinit var certificateService: CertificateService

    @Test
    fun `Get certificate list`() {
        val certificate1 = CertificateResourceBuilder().build()
        val certificate2 = CertificateResourceBuilder(id = "2", dn = ".atomhopper").build()
        every { certificateService.getCertificates() } returns listOf(certificate1, certificate2)

        webTestClient.queryGraphQL(queryResource = getCertificates, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("certificates.totalCount").isEqualTo(2)
    }
}
