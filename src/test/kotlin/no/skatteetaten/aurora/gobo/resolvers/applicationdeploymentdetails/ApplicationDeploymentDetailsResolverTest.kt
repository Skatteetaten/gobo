package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import assertk.assert
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.defaultInstant
import no.skatteetaten.aurora.gobo.healthResponseJson
import no.skatteetaten.aurora.gobo.infoResponseJson
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import org.junit.Ignore
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
import reactor.core.publisher.toMono
import java.time.Instant

@GraphQLTest
class ApplicationDeploymentDetailsResolverTest {

    @Value("classpath:graphql/getApplicationsWithRepositoriesAndTags.graphql")
    private lateinit var getRepositoriesAndTagsQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var applicationServiceBlocking: ApplicationServiceBlocking

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @BeforeEach
    fun setUp() {
        val affiliations = listOf("paas")

        val application = ApplicationResourceBuilder().build()
        given(applicationServiceBlocking.getApplications(affiliations))
            .willReturn(listOf(application))

        given(applicationServiceBlocking.getApplicationDeploymentDetails(anyString(), anyString()))
            .willReturn(ApplicationDeploymentDetailsBuilder().build())

        given(openShiftUserLoader.findOpenShiftUserByToken(anyString()))
            .willReturn(OpenShiftUserBuilder().build())
    }

    @AfterEach
    fun tearDown() = reset(applicationServiceBlocking, openShiftUserLoader)

    @Test
    fun `Query for repositories and tags`() {
        webTestClient.queryGraphQL(queryResource = getRepositoriesAndTagsQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody(QueryResponse.Response::class.java)
            .returnResult().let { result ->
                val applications = result.responseBody!!.data.applications
                val firstDeployment = applications.edges[0].node.applicationDeployments[0]
                val managementResponses = firstDeployment.details.podResources[0].managementResponses
                assert(managementResponses.health.textResponse).isEqualTo(healthResponseJson)
                assert(managementResponses.info.textResponse).isEqualTo(infoResponseJson)
            }
    }
}

class QueryResponse {
    data class HttpResponse(val textResponse: String)
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
