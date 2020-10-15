package no.skatteetaten.aurora.gobo.graphql.applicationdeploymentdetails

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.healthResponseJson
import no.skatteetaten.aurora.gobo.infoResponseJson
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.isFalse
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Disabled
class ApplicationDeploymentDetailsResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationsWithPods.graphql")
    private lateinit var getRepositoriesAndTagsQuery: Resource

    @MockkBean
    private lateinit var applicationServiceBlocking: ApplicationServiceBlocking

    @BeforeEach
    fun setUp() {
        val affiliations = listOf("paas")

        val application = ApplicationResourceBuilder().build()
        every { applicationServiceBlocking.getApplications(affiliations) } returns
            listOf(application)

        every { applicationServiceBlocking.getApplicationDeploymentDetails(any(), any()) } returns
            ApplicationDeploymentDetailsBuilder().build()
    }

    @Test
    fun `Query for deployments and pod status`() {
        webTestClient.queryGraphQL(queryResource = getRepositoriesAndTagsQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("applications.edges[0].node.applicationDeployments[0].details") {
                graphqlData("serviceLinks[0].name").isEqualTo("metrics")
                graphqlData("podResources[0].deployTag").isEqualTo("tag")
                graphqlData("podResources[0].phase").isEqualTo("status")
                graphqlData("podResources[0].ready").isFalse()
                graphqlData("podResources[0].startTime").hasJsonPath()
                graphqlData("podResources[0].restartCount").isEqualTo(3)
                graphqlData("podResources[0].containers.length()").isEqualTo(2)
                graphqlData("podResources[0].containers[0].restartCount").isEqualTo(1)
                graphqlData("podResources[0].containers[1].restartCount").isEqualTo(2)

                graphqlData("podResources[0].managementResponses.health.textResponse").isEqualTo(healthResponseJson)
                graphqlData("podResources[0].managementResponses.info.textResponse").isEqualTo(infoResponseJson)
            }
            .graphqlDoesNotContainErrors()
    }
}
