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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.TestPropertySource

@Import(CredentialMutation::class)
abstract class CredentialMutationTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/mutations/registerPostgresMotelServer.graphql")
    protected lateinit var registerPostgresMotelMutation: Resource

    protected val postgresMotelInput = PostgresMotelInput("test0oup-pgsql02", "username", "password", "bg")
    val registerPostgresVariables = mapOf(
        "input" to jacksonObjectMapper().convertValue<Map<String, Any>>(postgresMotelInput)
    )

    protected val messageSlot = slot<List<DetailedMessage>>()

    @MockkBean
    private lateinit var naghubService: NagHubService

    @BeforeEach
    fun setup() {
        coEvery {
            naghubService.sendMessage(any(), capture(messageSlot), any())
        } returns NagHubResult(true)
    }
}

@WithMockUser(
    authorities = ["testAdGroup"]
)
class AuthorizedTokenCredentialMutation : CredentialMutationTest() {
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
        assertThat(message.text).contains("DBH needs to be redeployed in cluster=http://localhost")
        assertThat(message.color).isEqualTo(NagHubColor.Yellow)
    }
}

class UnauthorizedTokenCredentialMutation : CredentialMutationTest() {

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
            .graphqlErrorsFirstContainsMessage("You do not have access")
    }
}

@WithMockUser(
    authorities = ["testAdGroup"]
)
@TestPropertySource(
    properties = ["integrations.herkimer.url=false"]
)
@Import(HerkimerServiceReactive::class, HerkimerServiceDisabled::class)
class UnavailableHerkimerCredentialMutation : CredentialMutationTest() {

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

@WithMockUser(
    authorities = ["testAdGroup"]
)
@TestPropertySource(
    properties = ["integrations.dbh.application.deployment.id=false"]
)
class DbhApplicationDeploymentIdNotSetCredentialMutation : CredentialMutationTest() {

    @Test
    fun `verify mutation is disabled when dbh adid is not present`() {
        webTestClient.queryGraphQL(
            queryResource = registerPostgresMotelMutation,
            variables = registerPostgresVariables,
            token = "test-token"
        ).expectStatus().isOk
            .expectBody()
            .graphqlErrorsFirstContainsMessage("Unknown type PostgresMotelInput")
    }
}
