package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.healthResponseJson
import no.skatteetaten.aurora.gobo.infoResponseJson
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ContainerResource
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
class ApplicationDeploymentDetailsResolverTest {

    @Value("classpath:graphql/queries/getApplicationsWithPods.graphql")
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
    fun `Query for deployments and pod status`() {
        webTestClient.queryGraphQL(queryResource = getRepositoriesAndTagsQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody(QueryResponse.Response::class.java)
            .returnResult().let { result ->
                val applications = result.responseBody!!.data.applications
                val firstDeployment = applications.edges[0].node.applicationDeployments[0]
                val pod = firstDeployment.details.podResources[0]
                val managementResponses = pod.managementResponses
                assertThat(managementResponses.health.textResponse).isEqualTo(healthResponseJson)
                assertThat(managementResponses.info.textResponse).isEqualTo(infoResponseJson)
                assertThat(pod.deployTag).isEqualTo("tag")
                assertThat(pod.phase).isEqualTo("status")
                assertThat(pod.ready).isFalse()
                assertThat(pod.restartCount).isEqualTo(3)
                assertThat(pod.containers.size).isEqualTo(2)
                assertThat(pod.containers[0].restartCount).isEqualTo(1)
                assertThat(pod.containers[1].restartCount).isEqualTo(2)
            }
    }
}

class QueryResponse {
    data class HttpResponse(val textResponse: String)
    data class ManagementResponses(val info: HttpResponse, val health: HttpResponse)
    data class PodResource(
        val managementResponses: ManagementResponses,
        val phase: String,
        val deployTag: String,
        val ready: Boolean,
        val restartCount: Int,
        val containers: List<ContainerResource>
    )

    data class ApplicationDetails(val podResources: List<PodResource>)
    data class ApplicationDeployment(val details: ApplicationDetails)
    data class Application(val applicationDeployments: List<ApplicationDeployment>)
    data class ApplicationEdge(val node: Application)
    data class ApplicationConnection(val edges: List<ApplicationEdge>)
    data class Applications(val applications: ApplicationConnection)
    data class Response(val data: Applications)
}
