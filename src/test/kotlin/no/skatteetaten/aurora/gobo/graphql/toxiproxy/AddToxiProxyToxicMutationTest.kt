package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import org.springframework.core.io.Resource
import com.ninjasquad.springmockk.MockkBean
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.printResult
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

@Import(ToxiProxyToxicMutation::class)
class AddToxiProxyToxicMutationTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/mutations/addToxiProxyToxic.graphql")
    private lateinit var addToxiProxyToxicMutation: Resource

    @MockkBean
    private lateinit var kubernetesCoroutinesClient: KubernetesCoroutinesClient

    @MockkBean(relaxed = true)
    private lateinit var toxiProxyService: ToxiProxyToxicService

    @Test
    fun `add toxic on existing toxi-proxy`() {

        val proxyPostResponse = """ 
            {
                "toxiProxyName": "test_toxiProxy",
                "toxicName": "latency_downstream"
            }
        """.trimIndent()

        val addToxiProxyToxicsInput = getToxiProxyToxicsInput()

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

    private fun getToxiProxyToxicsInput(): AddToxiProxyToxicsInput {
        val attr = listOf(
            ToxicAttributeInput("key", "latency"),
            ToxicAttributeInput("value", "1234")
        )
        val toxics = ToxicInput(
            name = "latency_downstream",
            type = "latency",
            stream = "downstream",
            toxicity = 1,
            attributes = attr
        )
        val toxiProxyInput = ToxiProxyInput(name = "test_toxiProxy", toxics = toxics)
        val addToxiProxyToxicsInput = AddToxiProxyToxicsInput(
            affiliation = "test_aff",
            environment = "test_env",
            application = "test_app",
            toxiProxy = toxiProxyInput
        )
        return addToxiProxyToxicsInput
    }

    private fun createApplicationDeployments(environment: String, vararg names: String) =
        names.map {
            ApplicationDeploymentResourceBuilder(
                affiliation = "aurora",
                environment = environment,
                name = it
            ).build()
        }
}
