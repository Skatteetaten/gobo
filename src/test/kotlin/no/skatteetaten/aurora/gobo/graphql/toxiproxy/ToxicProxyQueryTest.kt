package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsResourceBuilder
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentQuery
import no.skatteetaten.aurora.gobo.graphql.printResult
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

@Import(
    ApplicationDeploymentQuery::class,
    ToxiProxyDataLoader::class,
    ToxicProxyQueryTest.TestConfig::class
)
class ToxicProxyQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getToxiProxyToxics.graphql")
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

        coEvery {
            applicationService.getApplicationDeployment(any())
        } returns ApplicationDeploymentResourceBuilder().build()

        coEvery {
            applicationService.getApplicationDeploymentDetails(any(), any())
        } returns ApplicationDeploymentDetailsResourceBuilder().build()

        val proxyGetResponse = """ {
                "cluster" : {"value" : "myCluster"},  
                "envName" : {"value" : "env"},  
                "name"    : {"value" : "myName"},  
                "version" : {"value" : "1.0"}
            } """

        server.execute(proxyGetResponse) { // TODO sett inn json response fra toxiproxy her
            webTestClient.queryGraphQL(getApplicationDeploymentWithToxicsQuery, variables = mapOf("id" to "abc"), token = "test-token")
                .expectStatus().isOk
                .expectBody()
                .printResult()
        }

/*
            .graphqlDataWithPrefix("affiliations.edges") {
                graphqlData("[0].node.name").isEqualTo("paas")
                graphqlData("[1].node.name").isEqualTo("demo")
                graphqlData("[2].node.name").isEqualTo("notDeployed")
*/
    }
}
