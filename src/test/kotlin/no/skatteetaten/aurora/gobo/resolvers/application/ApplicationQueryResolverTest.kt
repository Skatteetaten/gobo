package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class ApplicationQueryResolverTest {

    @Value("classpath:graphql/getApplications.graphql")
    private lateinit var getApplicationsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationService: ApplicationService

    @Test
    fun `Query for applications given affiliations`() {
        val affiliations = listOf("paas")
        given(applicationService.getApplications(affiliations))
            .willReturn(listOf(ApplicationResourceBuilder().build()))

        val variables = mapOf("affiliations" to affiliations)
        val query = createQuery(getApplicationsQuery, variables)
        webTestClient
            .post()
            .uri("/graphql")
            .body(BodyInserters.fromObject(query))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.applications.totalCount").isNumber
            .jsonPath("$.data.applications.edges[0].node.name").isNotEmpty
            .jsonPath("$.data.applications.edges[0].node.affiliation.name").isNotEmpty
            .jsonPath("$.data.applications.edges[0].node.namespace.name").isNotEmpty
    }
}