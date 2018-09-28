package no.skatteetaten.aurora.gobo.resolvers.application

import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.createQuery
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono

@GraphQLTest
class ApplicationQueryResolverTest {
    private val firstApplicationDeployment = "\$.data.applications.edges[0].node.applicationDeployments[0]"

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

        given(applicationService.getApplicationDeploymentDetails(anyString()))
            .willReturn(Mono.just(ApplicationDeploymentDetailsBuilder().build()))

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
            .jsonPath("$firstApplicationDeployment.affiliation.name").isNotEmpty
            .jsonPath("$firstApplicationDeployment.namespace.name").isNotEmpty
            .jsonPath("$firstApplicationDeployment.details.buildTime").isNotEmpty
    }
}