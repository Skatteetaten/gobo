package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.createQuery
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@GraphQLTest
class AffiliationQueryResolverTest {

    @Value("classpath:graphql/getAffiliations.graphql")
    private lateinit var getAffiliationsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var affiliationService: AffiliationService

    @MockBean
    private lateinit var applicationService: ApplicationService

    @AfterEach
    fun tearDown() = reset(affiliationService, applicationService)

    @Test
    fun `Query for all affiliations`() {
        val affiliations = listOf("paas", "demo")
        given(affiliationService.getAllAffiliations()).willReturn(affiliations)
        given(applicationService.getApplications(affiliations)).willReturn(listOf(ApplicationResourceBuilder().build()))

        val query = createQuery(getAffiliationsQuery)
        webTestClient
            .post()
            .uri("/graphql")
            .body(BodyInserters.fromObject(query))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.affiliations.totalCount").isNumber
            .jsonPath("$.data.affiliations.edges[0].node.name").isNotEmpty
    }
}