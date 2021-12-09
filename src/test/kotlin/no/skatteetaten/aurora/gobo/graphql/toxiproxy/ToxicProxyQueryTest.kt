package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsResourceBuilder
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentQuery
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix

@Import(
    ApplicationDeploymentQuery::class,
    ToxiProxyDataLoader::class,
    ToxicProxyQueryTest.TestConfig::class
)
class ToxicProxyQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeploymentWithToxics.graphql")
    private lateinit var getApplicationDeploymentWithToxicsQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @Autowired
    private lateinit var server: MockWebServer

    @TestConfiguration
    class TestConfig {
        @Bean
        fun server() = MockWebServer()

        @Bean
        fun kubernetesCoroutinesClient(server: MockWebServer): KubernetesCoroutinesClient {
            return KubernetesCoroutinesClient(server.url, "test-token")
        }
    }

    @Test
    fun `Query for applications with toxics`() {

        coEvery { applicationService.getApplicationDeployment(any<String>()) } returns ApplicationDeploymentResourceBuilder(
            id = "123",
            msg = "Hei"
        ).build()

        coEvery {
            applicationService.getApplicationDeploymentDetails(any(), any())
        } returns ApplicationDeploymentDetailsResourceBuilder().build()

        val proxyGetResponse = """ 
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
              }
            }
        """.trimIndent()

        server.execute(proxyGetResponse) { // TODO sett inn json response fra toxiproxy her
            webTestClient.queryGraphQL(getApplicationDeploymentWithToxicsQuery, variables = mapOf("id" to "abc"), token = "test-token")
                .expectStatus().isOk
                .expectBody()
                // .printResult()
                .graphqlDataWithPrefix("applicationDeployment.toxiProxy") {
                    graphqlData("[0].podName").isEqualTo("name")
                    graphqlData("[0].name").isEqualTo("app")
                    graphqlData("[0].listen").isEqualTo("[::]:8090")
                    graphqlData("[0].upstream").isEqualTo("0.0.0.0:8080")
                    graphqlData("[0].enabled").isEqualTo("true")
                    graphqlData("[0].toxics[0].name").isEqualTo("latency_downstream_6")
                    graphqlData("[0].toxics[0].type").isEqualTo("latency")
                    graphqlData("[0].toxics[0].stream").isEqualTo("downstream")
                    graphqlData("[0].toxics[0].toxicity").isEqualTo("1")
                    graphqlData("[0].toxics[0].attributes[0].key").isEqualTo("latency")
                    graphqlData("[0].toxics[0].attributes[0].value").isEqualTo("855")
                    graphqlData("[0].toxics[0].attributes[1].key").isEqualTo("jitter")
                    graphqlData("[0].toxics[0].attributes[1].value").isEqualTo("455")
                }
        }
    }
}
