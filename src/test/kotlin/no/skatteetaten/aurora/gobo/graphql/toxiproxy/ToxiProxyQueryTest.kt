package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsResourceBuilder
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.ContainerResourceListBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.application.ApplicationDataLoader
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentQuery
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.mokey.PodResourceResource
import no.skatteetaten.aurora.gobo.service.AffiliationService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(
    AffiliationQuery::class,
    ApplicationDataLoader::class,
    ApplicationDeploymentQuery::class,
    ToxiProxyDataLoader::class,
    ToxiProxyQueryTest.TestConfig::class
)
class ToxiProxyQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeploymentWithToxics.graphql")
    private lateinit var getApplicationDeploymentWithToxicsQuery: Resource

    @Value("classpath:graphql/queries/getApplicationDeploymentAndToxiProxyWithAff.graphql")
    private lateinit var getApplicationDeploymentAndToxiProxyWithAff: Resource

    @Value("classpath:graphql/queries/getApplicationDeploymentAndToxiProxyWithRef.graphql")
    private lateinit var getApplicationDeploymentAndToxiProxyWithRef: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @Autowired
    private lateinit var server: MockWebServer

    private val proxyGetResponse = """ 
            {
              "app": {
                "name": "app",
                "listen": "[::]:8090",
                "upstream": "0.0.0.0:8080",
                "enabled": true,
                "toxics": [
                  {
                    "attributes": {
                      "latency": 855,
                      "jitter": 455
                    },
                    "name": "latency_downstream_6",
                    "type": "latency",
                    "stream": "downstream",
                    "toxicity": 1
                  }
                ]
              },
              "endpoint_TEST_ADDRESS": {
                "name": "endpoint_TEST_ADDRESS",
                "listen": "[::]:18000",
                "upstream": "test.no:80",
                "enabled": true,
                "toxics": []
              },
              "testproxy": {
                "name": "testproxy",
                "listen": "[::]:18001",
                "upstream": "enannentest.no:443",
                "enabled": true,
                "toxics": []
              }
            }
    """.trimIndent()

    private val proxyGetErrorResponse = """ 
            {
             "errors": [
                {
                  "message": "ToxiProxy 'name' failed"
                }
             ]
            }
    """.trimIndent()

    @TestConfiguration
    class TestConfig {
        @Bean
        fun server() = MockWebServer()

        @Bean
        fun kubernetesCoroutinesClient(server: MockWebServer): KubernetesCoroutinesClient {
            return KubernetesCoroutinesClient(server.url, "test-token")
        }
    }

    @BeforeEach
    fun setUp() {
        coEvery { applicationService.getApplicationDeployment(any()) } returns ApplicationDeploymentResourceBuilder(
            id = "123",
            msg = "Hei"
        ).build()
    }

    @Test
    fun `Query for applications with toxics`() {
        coEvery { applicationService.getApplicationDeploymentDetails(any(), any()) } returns ApplicationDeploymentDetailsResourceBuilder().build()

        server.execute(proxyGetResponse) {
            webTestClient.queryGraphQL(getApplicationDeploymentWithToxicsQuery, variables = mapOf("id" to "abc"), token = "test-token")
                .expectStatus().isOk
                .expectBody()
                .graphqlDataWithPrefix("applicationDeployment.toxiProxy") {
                    graphqlData("[0].podName").isEqualTo("name")
                    graphqlData("[0].name").isEqualTo("app")
                    graphqlData("[0].listen").isEqualTo("[::]:8090")
                    graphqlData("[0].upstream").isEqualTo("0.0.0.0:8080")
                    graphqlData("[0].enabled").isEqualTo("true")
                    graphqlData("[0].toxics[0].name").isEqualTo("latency_downstream_6")
                    graphqlData("[0].toxics[0].type").isEqualTo("latency")
                    graphqlData("[0].toxics[0].stream").isEqualTo("downstream")
                    graphqlData("[0].toxics[0].toxicity").isEqualTo("1.0")
                    graphqlData("[0].toxics[0].attributes[0].key").isEqualTo("latency")
                    graphqlData("[0].toxics[0].attributes[0].value").isEqualTo("855")
                    graphqlData("[0].toxics[0].attributes[1].key").isEqualTo("jitter")
                    graphqlData("[0].toxics[0].attributes[1].value").isEqualTo("455")
                }
                .graphqlData("applicationDeployment.toxiProxy.length()").isEqualTo(3)
                .graphqlDoesNotContainErrors()
        }
    }

    @Test
    fun `Query for applications for toxics returning partial result`() {
        val podResource = PodResourceResource(
            name = "name",
            phase = "status",
            deployTag = "tag",
            latestDeployTag = true,
            replicaName = "deployment-1",
            latestReplicaName = true,
            containers = ContainerResourceListBuilder().build(),
            managementResponses = null
        )
        val appDetails = ApplicationDeploymentDetailsResourceBuilder().build().copy(podResources = listOf(podResource, podResource))
        coEvery { applicationService.getApplicationDeploymentDetails(any(), any()) } returns appDetails

        server.execute(proxyGetResponse, proxyGetErrorResponse) {
            webTestClient.queryGraphQL(getApplicationDeploymentWithToxicsQuery, variables = mapOf("id" to "abc"), token = "test-token")
                .expectStatus().isOk
                .expectBody()
                .graphqlData("applicationDeployment.toxiProxy[0].podName").isEqualTo("name")
                .graphqlErrorsFirst("message").isEqualTo("ToxiProxy 'name' failed")
        }
    }

    @Test
    fun `Query for applications and toxics given affiliation`() {
        coEvery { applicationService.getApplications(any(), any()) } returns
            listOf(
                ApplicationResourceBuilder(
                    affiliation = "my_aff",
                    applicationDeployments =
                    listOf(
                        ApplicationDeploymentResourceBuilder(affiliation = "my_aff", environment = "my_env", name = "my_app").build(),
                    )
                ).build()
            )
        coEvery { applicationService.getApplicationDeploymentDetails(any(), any()) } returns ApplicationDeploymentDetailsResourceBuilder().build()

        server.execute(proxyGetResponse) {
            webTestClient.queryGraphQL(getApplicationDeploymentAndToxiProxyWithAff, token = "test-token")
                .expectStatus().isOk
                .expectBody()
                .graphqlDataWithPrefix("affiliations.edges[0].node") {
                    graphqlData("name").isEqualTo("my_aff")
                    // graphqlData("applications[0].applicationDeployments.length()").isEqualTo(1)
                    graphqlData("applications[0].applicationDeployments[0].environment").isEqualTo("my_env")
                    graphqlData("applications[0].applicationDeployments[0].toxiProxy.length()").isEqualTo(3)
                }
        }
    }
    @Test
    fun `Query for applications and toxics with refs`() {
        coEvery { applicationService.getApplications(any(), any()) } returns
            listOf(
                ApplicationResourceBuilder(
                    affiliation = "my_aff",
                    applicationDeployments =
                    listOf(
                        ApplicationDeploymentResourceBuilder(affiliation = "my_aff", environment = "my_env", name = "my_app").build(),
                    )
                ).build()
            )
        coEvery { applicationService.getApplicationDeploymentDetails(any(), any()) } returns ApplicationDeploymentDetailsResourceBuilder().build()

        server.execute(proxyGetResponse) {
            webTestClient.queryGraphQL(getApplicationDeploymentAndToxiProxyWithRef, token = "test-token")
                .expectStatus().isOk
                .expectBody()
                .graphqlDataWithPrefix("affiliations.edges[0].node") {
                    graphqlData("name").isEqualTo("my_aff")
                    graphqlData("applications[0].applicationDeployments.length()").isEqualTo(1)
                }
        }
    }
}
