package no.skatteetaten.aurora.gobo.resolvers.certificate

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import no.skatteetaten.aurora.gobo.CertificateResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.integration.skap.CertificateService
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class CertificateResolverTest {

    @Value("classpath:graphql/queries/getCertificates.graphql")
    private lateinit var getCertificates: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @MockkBean
    private lateinit var certificateService: CertificateService

    @BeforeEach
    fun setUp() {
        every { openShiftUserLoader.findOpenShiftUserByToken(any()) } returns OpenShiftUserBuilder().build()
    }

    @AfterEach
    fun tearDown() = clearAllMocks()

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
