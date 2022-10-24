package no.skatteetaten.aurora.gobo.graphql.loadtest

import java.io.File
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import com.fasterxml.jackson.module.kotlin.convertValue
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import assertk.assertThat
import assertk.assertions.isEqualTo
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.AddOrUpdateToxiProxyInput
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.DeleteToxiProxyToxicsInput
import no.skatteetaten.aurora.gobo.graphql.toxiproxy.ToxiProxyToxicMutation
import no.skatteetaten.aurora.gobo.integration.toxiproxy.ToxiProxyToxicService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient

@Import(
    ToxiProxyToxicMutation::class,
)
class GoboLoadtestSimulationMutationTest : GraphQLTestWithDbhAndSkap() {

    @MockkBean
    private lateinit var kubernetesCoroutinesClient: KubernetesCoroutinesClient

    @MockkBean(relaxed = true)
    private lateinit var toxiProxyService: ToxiProxyToxicService

    data class QueryContent(
        var operationName: String?,
        var variables: String,
        var query: String
    )

    @Test
    fun `verify mutation contents add delay toxic`() {
        val addToxiProxyToxicsInput = "src/gatling/resources/mokey_add_delay_toxic_mutation.json".toJsonType<AddOrUpdateToxiProxyInput>()
        assertThat(addToxiProxyToxicsInput.affiliation).isEqualTo("aup")
        assertThat(addToxiProxyToxicsInput.toxiProxy.toxics.name).isEqualTo("delay_toxic")
    }
    @Test
    fun `verify mutation contents add latency toxic`() {
        val addToxiProxyToxicsInput = "src/gatling/resources/mokey_add_latency_toxic_mutation.json".toJsonType<AddOrUpdateToxiProxyInput>()
        assertThat(addToxiProxyToxicsInput.affiliation).isEqualTo("aup")
        assertThat(addToxiProxyToxicsInput.toxiProxy.toxics.name).isEqualTo("latency_toxic")
    }
    @Test
    fun `verify mutation contents add timeout toxic`() {
        val addToxiProxyToxicsInput = "src/gatling/resources/mokey_add_timeout_toxic_mutation.json".toJsonType<AddOrUpdateToxiProxyInput>()
        assertThat(addToxiProxyToxicsInput.affiliation).isEqualTo("aup")
        assertThat(addToxiProxyToxicsInput.toxiProxy.toxics.name).isEqualTo("timeout_toxic")
    }
    @Test
    fun `verify mutation contents delete delay toxic`() {
        val deleteToxiProxyToxicsInput = "src/gatling/resources/mokey_delete_delay_toxic_mutation.json".toJsonType<DeleteToxiProxyToxicsInput>()
        assertThat(deleteToxiProxyToxicsInput.affiliation).isEqualTo("aup")
        assertThat(deleteToxiProxyToxicsInput.environment).isEqualTo("utv01")
        assertThat(deleteToxiProxyToxicsInput.toxiProxyName).isEqualTo("mokeyToxic")
        assertThat(deleteToxiProxyToxicsInput.toxicName).isEqualTo("delay_toxic")
    }
    @Test
    fun `verify mutation contents delete latency toxic`() {
        val deleteToxiProxyToxicsInput = "src/gatling/resources/mokey_delete_latency_toxic_mutation.json".toJsonType<DeleteToxiProxyToxicsInput>()
        assertThat(deleteToxiProxyToxicsInput.affiliation).isEqualTo("aup")
        assertThat(deleteToxiProxyToxicsInput.environment).isEqualTo("utv01")
        assertThat(deleteToxiProxyToxicsInput.toxiProxyName).isEqualTo("mokeyToxic")
        assertThat(deleteToxiProxyToxicsInput.toxicName).isEqualTo("latency_toxic")
    }
    @Test
    fun `verify mutation contents delete timeout toxic`() {
        val deleteToxiProxyToxicsInput = "src/gatling/resources/mokey_delete_timeout_toxic_mutation.json".toJsonType<DeleteToxiProxyToxicsInput>()
        assertThat(deleteToxiProxyToxicsInput.affiliation).isEqualTo("aup")
        assertThat(deleteToxiProxyToxicsInput.environment).isEqualTo("utv01")
        assertThat(deleteToxiProxyToxicsInput.toxiProxyName).isEqualTo("mokeyToxic")
        assertThat(deleteToxiProxyToxicsInput.toxicName).isEqualTo("timeout_toxic")
    }

    @Test
    fun `add delay toxic on toxi-proxy`() {
        val queryValue = "src/gatling/resources/mokey_add_delay_toxic_mutation.json".jsonInputQuery
        val inputVariables = "src/gatling/resources/mokey_add_delay_toxic_mutation.json".jsonInputVariables
        webTestClient.queryGraphQL(queryValue, inputVariables, "test-token").expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("addToxiProxyToxic") {
                graphqlData("toxiProxyName").isEqualTo("mokeyToxic")
                graphqlData("toxicName").isEqualTo("delay_toxic")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `add latency toxic on toxi-proxy`() {
        val queryValue = "src/gatling/resources/mokey_add_latency_toxic_mutation.json".jsonInputQuery
        val inputVariables = "src/gatling/resources/mokey_add_latency_toxic_mutation.json".jsonInputVariables
        webTestClient.queryGraphQL(queryValue, inputVariables, "test-token").expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("addToxiProxyToxic") {
                graphqlData("toxiProxyName").isEqualTo("mokeyToxic")
                graphqlData("toxicName").isEqualTo("latency_toxic")
            }
            .graphqlDoesNotContainErrors()
    }
    @Test
    fun `add timeout toxic on toxi-proxy`() {
        val queryValue = "src/gatling/resources/mokey_add_timeout_toxic_mutation.json".jsonInputQuery
        val inputVariables = "src/gatling/resources/mokey_add_timeout_toxic_mutation.json".jsonInputVariables
        webTestClient.queryGraphQL(queryValue, inputVariables, "test-token").expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("addToxiProxyToxic") {
                graphqlData("toxiProxyName").isEqualTo("mokeyToxic")
                graphqlData("toxicName").isEqualTo("timeout_toxic")
            }
            .graphqlDoesNotContainErrors()
    }

    private val String.jsonInputQuery get() = jacksonObjectMapper().readTree(File(this)).at("/query").asText()

    private val String.jsonInputVariables get() = jacksonObjectMapper().readTree(File(this)).at("/variables/input")

    private inline fun <reified T> String.toJsonType(): T =
        jacksonObjectMapper().convertValue(jacksonObjectMapper().readTree(File(this)).at("/variables/input"))
}
