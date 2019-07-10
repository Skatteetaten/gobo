package no.skatteetaten.aurora.gobo.resolvers.applicationdeploymentdetails

import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.healthResponseJson
import no.skatteetaten.aurora.gobo.infoResponseJson
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.isFalse
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
            .expectBody()
            .graphqlDataWithPrefix("applications.edges[0].node.applicationDeployments[0].details.podResources[0]") {
                graphqlData("deployTag").isEqualTo("tag")
                graphqlData("phase").isEqualTo("status")
                graphqlData("ready").isFalse()
                graphqlData("startTime").hasJsonPath()
                graphqlData("restartCount").isEqualTo(3)
                graphqlData("containers.length()").isEqualTo(2)
                graphqlData("containers[0].restartCount").isEqualTo(1)
                graphqlData("containers[1].restartCount").isEqualTo(2)

                graphqlData("managementResponses.health.textResponse").isEqualTo(healthResponseJson)
                graphqlData("managementResponses.info.textResponse").isEqualTo(infoResponseJson)
            }
    }
}
