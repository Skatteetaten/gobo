package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.affiliation.AffiliationService
import no.skatteetaten.aurora.gobo.application.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.createQuery
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["management.server.port=-1"])
@DirtiesContext
class AffiliationQueryResolverTest {

    @Value("classpath:graphql/getAffiliations.graphql")
    private lateinit var getAffiliationsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var affiliationService: AffiliationService

    @MockBean
    private lateinit var applicationService: ApplicationService

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