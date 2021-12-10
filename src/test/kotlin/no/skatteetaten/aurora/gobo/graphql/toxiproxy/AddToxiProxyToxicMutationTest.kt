package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import org.springframework.core.io.Resource
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentDetailsResourceBuilder
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentQuery
import no.skatteetaten.aurora.gobo.graphql.printResult
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.execute
import no.skatteetaten.aurora.mockmvc.extensions.mockwebserver.url
import okhttp3.mockwebserver.MockWebServer

@AutoConfigureStubRunner(ids = ["no.skatteetaten.aurora:mokey:3.3.4:stubs:6565"])
@Import(
    ApplicationDeploymentQuery::class,
    ToxiProxyToxicService::class,
    ToxiProxyToxicMutation::class,
    AddToxiProxyToxicsInput::class,
    ToxiProxyInput::class,
    ToxicInput::class,
    AddToxiProxyToxicMutationTest.TestConfig::class
)
class AddToxiProxyToxicMutationTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/mutations/addToxiProxyToxic.graphql")
    private lateinit var addToxiProxyToxicMutation: Resource

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

    @BeforeEach
    fun setUp() {
        coEvery { applicationService.getApplicationDeployment(any()) } returns ApplicationDeploymentResourceBuilder(
            id = "123",
            msg = "Hei"
        ).build()

        coEvery {
            applicationService.getApplicationDeploymentDetails(any(), any())
        } returns ApplicationDeploymentDetailsResourceBuilder().build()
    }

    @Test
    fun `add toxic on existing toxi-proxy`() {

        val proxyPostResponse = """ 
            {
                "toxiProxyName": "test_toxiProxy",
                "toxicName": "latency_downstream"
            }
        """.trimIndent()

        server.execute(proxyPostResponse) {
            val attr = listOf(
                ToxicAttributeInput("key", "latency"),
                ToxicAttributeInput("value", "1234")
            )
            val toxics = ToxicInput(name = "latency_downstream", type = "latency", stream = "downstream", toxicity = 1, attributes = attr)
            val toxiProxyInput = ToxiProxyInput(name = "test_toxiProxy", toxics = toxics)
            val addToxiProxyToxicsInput = AddToxiProxyToxicsInput(
                affiliation = "test_aff",
                environment = "test_env",
                application = "test_app",
                toxiProxy = toxiProxyInput
            )

            webTestClient.queryGraphQL(addToxiProxyToxicMutation, addToxiProxyToxicsInput, "test-token")
                .expectStatus().isOk
                .expectBody()
                .printResult()

            // .graph
            //     graphqlData("deployId").isEqualTo("123")
            //     graphqlData("deploymentRef.cluster").isEqualTo("utv")
            //     graphqlData("deploymentRef.affiliation").isEqualTo("aurora")
            //     graphqlData("deploymentRef.environment").isEqualTo("dev-utv")
            //     graphqlData("deploymentRef.application").isEqualTo("gobo")
            //     graphqlData("timestamp").isNotEmpty
            //     graphqlData("message").isEmpty
            // }
            // .graphqlDoesNotContainErrors()
        }
    }
}
