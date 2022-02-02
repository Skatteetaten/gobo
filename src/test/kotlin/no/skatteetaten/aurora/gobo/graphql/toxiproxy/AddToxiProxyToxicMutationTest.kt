package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import org.springframework.core.io.Resource
import com.ninjasquad.springmockk.MockkBean
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

const val TOXY_PROXY_NAME = "test_toxiProxy"
const val TOXIC_NAME = "latency_downstream"

@Import(ToxiProxyToxicMutation::class)
class AddToxiProxyToxicMutationTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/mutations/addToxiProxyToxic.graphql")
    private lateinit var addToxiProxyToxicMutation: Resource

    @Value("classpath:graphql/mutations/deleteToxiProxyToxic.graphql")
    private lateinit var deleteToxiProxyToxicMutation: Resource

    @MockkBean
    private lateinit var kubernetesCoroutinesClient: KubernetesCoroutinesClient

    @MockkBean(relaxed = true)
    private lateinit var toxiProxyService: ToxiProxyToxicService

    @Test
    fun `add toxic on existing toxi-proxy`() {

        val addToxiProxyToxicsInput = getAddToxiProxyToxicsInput()

        webTestClient.queryGraphQL(addToxiProxyToxicMutation, addToxiProxyToxicsInput, "test-token")
            .expectStatus().isOk
            .expectBody()
            // .printResult()
            .graphqlDataWithPrefix("addToxiProxyToxic") {
                graphqlData("toxiProxyName").isEqualTo(TOXY_PROXY_NAME)
                graphqlData("toxicName").isEqualTo(TOXIC_NAME)
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `delete toxic with name`() {

        val deleteToxiProxyToxicsInput = getDeleteToxiProxyToxicsInput()

        webTestClient.queryGraphQL(deleteToxiProxyToxicMutation, deleteToxiProxyToxicsInput, "test-token")
            .expectStatus().isOk
            .expectBody()
            // .printResult()
            .graphqlDataWithPrefix("deleteToxiProxyToxic") {
                graphqlData("toxiProxyName").isEqualTo(TOXY_PROXY_NAME)
                graphqlData("toxicName").isEqualTo(TOXIC_NAME)
            }
            .graphqlDoesNotContainErrors()
    }

    private fun getAddToxiProxyToxicsInput(): AddOrUpdateToxiProxyInput {
        val attr = listOf(
            ToxicAttributeInput("key", "latency"),
            ToxicAttributeInput("value", "1234")
        )
        val toxics = ToxicInput(
            name = TOXIC_NAME,
            type = "latency",
            stream = "downstream",
            toxicity = 1,
            attributes = attr
        )
        val toxiProxyInput = ToxiProxyInput(name = TOXY_PROXY_NAME, toxics = toxics)
        val addToxiProxyToxicsInput = AddOrUpdateToxiProxyInput(
            affiliation = "test_aff",
            environment = "test_env",
            application = "test_app",
            toxiProxy = toxiProxyInput
        )
        return addToxiProxyToxicsInput
    }

    private fun getDeleteToxiProxyToxicsInput(): DeleteToxiProxyToxicsInput {
        val deleteToxiProxyToxicsInput = DeleteToxiProxyToxicsInput(
            affiliation = "test_aff",
            environment = "test_env",
            application = "test_app",
            toxiProxyName = TOXY_PROXY_NAME,
            toxicName = TOXIC_NAME
        )
        return deleteToxiProxyToxicsInput
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
