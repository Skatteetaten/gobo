package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.ApplicationInstanceDetailsBuilder
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["management.server.port=-1"])
@DirtiesContext
class ApplicationQueryResolverTest {
    private val firstApplicationInstance = "\$.data.applications.edges[0].node.applicationInstances[0]"

    @Value("classpath:graphql/getApplications.graphql")
    private lateinit var getApplicationsQuery: Resource

    @Value("classpath:graphql/invalidQuery.graphql")
    private lateinit var invalidQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationService: ApplicationService

    @Test
    fun `Query for applications given affiliations`() {
        val affiliations = listOf("paas")
        given(applicationService.getApplicationInstanceDetails(affiliations)).willReturn(
            listOf(
                ApplicationInstanceDetailsBuilder().build()
            )
        )
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
            .jsonPath("$firstApplicationInstance.affiliation.name").isNotEmpty
            .jsonPath("$firstApplicationInstance.namespace.name").isNotEmpty
            .jsonPath("$firstApplicationInstance.details.buildTime").isNotEmpty
    }

    @Test
    fun `Given invalid query return errors array`() {
        webTestClient
                .post()
                .uri("/graphql")
                .body(BodyInserters.fromObject(createQuery(invalidQuery)))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.errors").isArray
    }
}