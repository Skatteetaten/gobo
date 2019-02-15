package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.Base64Utils

@GraphQLTest
class AffiliationQueryResolverTest {

    @Value("classpath:graphql/queries/getAffiliations.graphql")
    private lateinit var getAffiliationsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var affiliationService: AffiliationServiceBlocking

    @MockBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @AfterEach
    fun tearDown() = reset(affiliationService, applicationService)

    @Test
    fun `Query for all affiliations`() {
        val affiliations = listOf("paas", "demo")
        given(affiliationService.getAllAffiliations()).willReturn(affiliations)
        given(applicationService.getApplications(affiliations)).willReturn(listOf(ApplicationResourceBuilder().build()))

        webTestClient.queryGraphQL(getAffiliationsQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.totalCount").isEqualTo(2)
            .graphqlData("affiliations.edges[0].node.name").isEqualTo("paas")
            .graphqlData("affiliations.edges[0].cursor").isEqualTo("paas".toBase64())
            .graphqlData("affiliations.edges[1].node.name").isEqualTo("demo")
            .graphqlData("affiliations.edges[1].cursor").isEqualTo("demo".toBase64())
    }

    private fun String.toBase64() = Base64Utils.encodeToString(this.toByteArray())
}