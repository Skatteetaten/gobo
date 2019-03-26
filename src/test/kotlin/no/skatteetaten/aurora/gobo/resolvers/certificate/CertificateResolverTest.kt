package no.skatteetaten.aurora.gobo.resolvers.certificate

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class CertificateResolverTest {

    @Value("classpath:graphql/queries/getCertificates.graphql")
    private lateinit var getCertificates: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        given(openShiftUserLoader.findOpenShiftUserByToken(anyString())).willReturn(OpenShiftUserBuilder().build())
    }

    @AfterEach
    fun tearDown() = reset(openShiftUserLoader)

    @Test
    fun `Get certificate list`() {
        webTestClient.queryGraphQL(queryResource = getCertificates, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("certificates.totalCount").isEqualTo(2)
    }
}