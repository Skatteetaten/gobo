package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import org.springframework.core.io.Resource
import com.ninjasquad.springmockk.MockkBean
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

const val TOXY_PROXY_NAME = "test_toxiProxy"
const val TOXIC_NAME = "latency_downstream"
const val TOXIC_PROXY_LISTEN = "\"[::]:8090\""
const val TOXIC_PROXY_UPSTREAM = "\"0.0.0.0:8080\""
const val TOXIC_PROXY_ENABLED = true

@Import(ToxiProxyToxicMutation::class)
class ToxiProxyToxicMutationTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/mutations/addToxiProxyToxic.graphql")
    private lateinit var addToxiProxyToxicMutation: Resource

    @Value("classpath:graphql/mutations/updateToxiProxy.graphql")
    private lateinit var updateToxiProxyMutation: Resource

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
            .graphqlDataWithPrefix("addToxiProxyToxic") {
                graphqlData("toxiProxyName").isEqualTo(TOXY_PROXY_NAME)
                graphqlData("toxicName").isEqualTo(TOXIC_NAME)
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `update toxic on toxi-proxy`() {

        val updateToxiProxyToxicsInput = getUpdateToxiProxyInput()

        webTestClient.queryGraphQL(updateToxiProxyMutation, updateToxiProxyToxicsInput, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("updateToxiProxy") {
                graphqlData("toxiProxyName").isEqualTo(TOXY_PROXY_NAME)
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `delete toxic with name`() {

        val deleteToxiProxyToxicsInput = getDeleteToxiProxyToxicsInput()

        webTestClient.queryGraphQL(deleteToxiProxyToxicMutation, deleteToxiProxyToxicsInput, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("deleteToxiProxyToxic") {
                graphqlData("toxiProxyName").isEqualTo(TOXY_PROXY_NAME)
                graphqlData("toxicName").isEqualTo(TOXIC_NAME)
            }
            .graphqlDoesNotContainErrors()
    }

    private fun getAddToxiProxyToxicsInput(): AddToxiProxyInput {
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
        val addToxiProxyToxicsInput = AddToxiProxyInput(
            affiliation = "test_aff",
            environment = "test_env",
            application = "test_app",
            toxiProxy = toxiProxyInput
        )
        return addToxiProxyToxicsInput
    }

    private fun getUpdateToxiProxyInput(): UpdateToxiProxyInput {
        val toxiProxyInput = ToxiProxyUpdate(
            name = TOXY_PROXY_NAME,
            listen = TOXIC_PROXY_LISTEN,
            upstream = TOXIC_PROXY_UPSTREAM,
            enabled = TOXIC_PROXY_ENABLED
        )
        val updateToxiProxyToxicsInput = UpdateToxiProxyInput(
            affiliation = "test_aff",
            environment = "test_env",
            application = "test_app",
            toxiProxy = toxiProxyInput
        )
        return updateToxiProxyToxicsInput
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
}
