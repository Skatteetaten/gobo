package no.skatteetaten.aurora.gobo.graphql.credentials

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.slot
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithoutDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.contains
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirstContainsMessage
import no.skatteetaten.aurora.gobo.graphql.isFalse
import no.skatteetaten.aurora.gobo.graphql.isTrue
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerResult
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerService
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerServiceDisabled
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerServiceReactive
import no.skatteetaten.aurora.gobo.integration.herkimer.RegisterResourceAndClaimCommand
import no.skatteetaten.aurora.gobo.integration.naghub.DetailedMessage
import no.skatteetaten.aurora.gobo.integration.naghub.NagHubColor
import no.skatteetaten.aurora.gobo.integration.naghub.NagHubResult
import no.skatteetaten.aurora.gobo.integration.naghub.NagHubService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.TestPropertySource

class CredentialMutationTest {

    @WithMockUser(authorities = ["testAdGroup"])
    @Nested
    inner class AuthorizedTokenCredentialMutation(
        @Value("\${openshift.cluster}") val cluster: String
    ) : CredentialMutationBaseTest() {
        @MockkBean
        private lateinit var herkimerService: HerkimerService

        @Test
        fun `Mutate credentials return false with message given response false`() {
            coEvery { herkimerService.registerResourceAndClaim(any()) } returns HerkimerResult(false)

            webTestClient.queryGraphQL(
                queryResource = registerPostgresMotelMutation,
                variables = registerPostgresVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlDataWithPrefix("registerPostgresMotelServer") {
                    graphqlData("success").isFalse()
                    graphqlData("message").contains("host=test0oup-pgsql02 could not be registered")
                }

            val message = messageSlot.captured.first()
            assertThat(message.text).contains("needs to be manually registered")
            assertThat(message.color).isEqualTo(NagHubColor.Red)
        }

        @Test
        fun `Mutate credentials return true given response success`() {

            val instanceNameSlot = slot<RegisterResourceAndClaimCommand>()
            coEvery {
                herkimerService.registerResourceAndClaim(capture(instanceNameSlot))
            } returns HerkimerResult(true)

            val expectedInstanceName = "bg-postgres-02"

            webTestClient.queryGraphQL(
                queryResource = registerPostgresMotelMutation,
                variables = registerPostgresVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlDataWithPrefix("registerPostgresMotelServer") {
                    graphqlData("success").isTrue()
                    graphqlData("message").contains(postgresMotelInput.host)
                }
                .graphqlDoesNotContainErrors()

            assertThat(instanceNameSlot.captured.resourceName).isEqualTo(expectedInstanceName)
            val message = messageSlot.captured.first()
            assertThat(message.text).contains("DBH needs to be redeployed in cluster=$cluster")
            assertThat(message.color).isEqualTo(NagHubColor.Yellow)
        }
    }

    @Nested
    inner class UnauthorizedTokenCredentialMutation : CredentialMutationBaseTest() {
        @MockkBean
        private lateinit var herkimerService: HerkimerService

        @Test
        fun `Error when unauthorized and registering credentials`() {
            webTestClient.queryGraphQL(
                queryResource = registerPostgresMotelMutation,
                variables = registerPostgresVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlErrorsFirstContainsMessage("Access denied, missing/invalid token or the token does not have the required permissions")
        }
    }

    @WithMockUser(authorities = ["wrongAdGroup"])
    @Nested
    inner class WrongAdGroupTokenCredentialMutation : CredentialMutationBaseTest() {
        @MockkBean(relaxed = true)
        private lateinit var herkimerService: HerkimerService

        @Test
        fun `Error when wrong ad group`() {
            webTestClient.queryGraphQL(
                queryResource = registerPostgresMotelMutation,
                variables = registerPostgresVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlErrorsFirstContainsMessage("Access denied, missing/invalid token or the token does not have the required permissions")
        }
    }

    @WithMockUser(authorities = ["testAdGroup"])
    @TestPropertySource(properties = ["integrations.herkimer.url=false"])
    @Nested
    @Import(HerkimerServiceReactive::class, HerkimerServiceDisabled::class)
    inner class UnavailableHerkimerCredentialMutation : CredentialMutationBaseTest() {

        @Test
        fun `verify herkimer is disabled when no url is set`() {
            webTestClient.queryGraphQL(
                queryResource = registerPostgresMotelMutation,
                variables = registerPostgresVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlErrorsFirstContainsMessage("Herkimer integration is disabled")
        }
    }

    @WithMockUser(authorities = ["testAdGroup"])
    @TestPropertySource(properties = ["integrations.dbh.application.deployment.id=false"])
    @Nested
    inner class DbhApplicationDeploymentIdNotSetCredentialMutation : CredentialMutationBaseTest() {

        @Test
        fun `verify mutation is disabled when dbh is not present`() {
            webTestClient.queryGraphQL(
                queryResource = registerPostgresMotelMutation,
                variables = registerPostgresVariables,
                token = "test-token"
            ).expectStatus().isOk
                .expectBody()
                .graphqlErrorsFirstContainsMessage("Unknown type PostgresMotelInput")
        }
    }
}

@Import(CredentialMutation::class)
open class CredentialMutationBaseTest : GraphQLTestWithoutDbhAndSkap() {
    @Value("classpath:graphql/mutations/registerPostgresMotelServer.graphql")
    lateinit var registerPostgresMotelMutation: Resource

    val postgresMotelInput = PostgresMotelInput("test0oup-pgsql02", "username", "password", "bg")
    val registerPostgresVariables = mapOf(
        "input" to jacksonObjectMapper().convertValue<Map<String, Any>>(postgresMotelInput)
    )

    val messageSlot = slot<List<DetailedMessage>>()

    @MockkBean
    lateinit var naghubService: NagHubService

    @BeforeEach
    fun setup() {
        coEvery {
            naghubService.sendMessage(any(), capture(messageSlot), any())
        } returns NagHubResult(true)
    }
}
