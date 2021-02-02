package no.skatteetaten.aurora.gobo.graphql.credentials

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithoutDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.contains
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirstContainsMessage
import no.skatteetaten.aurora.gobo.graphql.isFalse
import no.skatteetaten.aurora.gobo.graphql.isTrue
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerResult
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerService
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerServiceDisabled
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerServiceReactive
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

    val registerPostgresVariables = mapOf(
        "input" to jacksonObjectMapper().convertValue<Map<String, Any>>(
            PostgresMotelInput("test0oup", "username", "password", "bg")
        )
    )
}

@WithMockUser(
    username = "system:serviceaccount:aurora:vra"
)
class AuthorizedTokenCredentialMutation : CredentialMutationTest() {
    @MockkBean
    private lateinit var herkimerService: HerkimerService

    @Test
    fun `Mutate database schema return true given response success`() {
        coEvery { herkimerService.registerResourceAndClaim(any()) } returns HerkimerResult(true)

        webTestClient.queryGraphQL(
            queryResource = registerPostgresMotelMutation,
            variables = registerPostgresVariables,
            token = "test-token"
        ).expectStatus().isOk
            .expectBody()
            .graphqlData("registerPostgresMotelServer").isNotEmpty
            .graphqlData("registerPostgresMotelServer.success").isTrue()
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Mutate database schema return false with message given response false`() {
        coEvery { herkimerService.registerResourceAndClaim(any()) } returns HerkimerResult(false)

        webTestClient.queryGraphQL(
            queryResource = registerPostgresMotelMutation,
            variables = registerPostgresVariables,
            token = "test-token"
        ).expectStatus().isOk
            .expectBody()
            .graphqlData("registerPostgresMotelServer").isNotEmpty
            .graphqlData("registerPostgresMotelServer.success").isFalse()
            .graphqlData("registerPostgresMotelServer.message").contains("host=test0oup could not be registered")
    }
}

class UnauthorizedTokenCredentialMutation : CredentialMutationTest() {
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
    username = "system:serviceaccount:aurora:vra"
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
