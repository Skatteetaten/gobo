package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import assertk.assert
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.defaultInstant
import no.skatteetaten.aurora.gobo.healthResponseJson
import no.skatteetaten.aurora.gobo.infoResponseJson
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.resolvers.createQuery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.Instant

@GraphQLTest
class ApplicationDeploymentDetailsResolverTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationService: ApplicationService

    @BeforeEach
    fun setUp() {
        val affiliations = listOf("paas")
        given(applicationService.getApplicationDeploymentDetails(affiliations))
            .willReturn(listOf(ApplicationDeploymentDetailsBuilder().build()))
        given(applicationService.getApplications(affiliations))
            .willReturn(listOf(ApplicationResourceBuilder().build()))
    }

    @Test
    fun `Query for repositories and tags`() {

        val queryString = """{
  applications(affiliations: ["paas"]) {
    edges {
      node {
        applicationDeployments {
          details {
            podResources {
              managementResponses {
                info {
                  textResponse
                  loadedTime
                }
                health {
                  textResponse
                  loadedTime
                }
              }
            }
          }
        }
      }
    }
  }
}
"""
        val query = createQuery(queryString)

        webTestClient
            .post()
            .uri("/graphql")
            .body(BodyInserters.fromObject(query))
            .exchange()
            .expectStatus().isOk
            .expectBody(QueryResponse.Response::class.java)
            .consumeWith<Nothing> { result ->
                val applications = result.responseBody!!.data.applications
                val firstDeployment = applications.edges[0].node.applicationDeployments[0]
                val managementResponses = firstDeployment.details.podResources[0].managementResponses
                assert(managementResponses.health.textResponse).isEqualTo(healthResponseJson)
                assert(managementResponses.health.loadedTime).isEqualTo(defaultInstant)
                assert(managementResponses.info.textResponse).isEqualTo(infoResponseJson)
                assert(managementResponses.info.loadedTime).isEqualTo(defaultInstant)
            }
    }
}

class QueryResponse {
    data class HttpResponse(val textResponse: String, val loadedTime: Instant)
    data class ManagementResponses(val info: HttpResponse, val health: HttpResponse)
    data class PodResource(val managementResponses: ManagementResponses)
    data class ApplicationDetails(val podResources: List<PodResource>)
    data class ApplicationDeployment(val details: ApplicationDetails)
    data class Application(val applicationDeployments: List<ApplicationDeployment>)
    data class ApplicationEdge(val node: Application)
    data class ApplicationConnection(val edges: List<ApplicationEdge>)
    data class Applications(val applications: ApplicationConnection)
    data class Response(val data: Applications)
}
