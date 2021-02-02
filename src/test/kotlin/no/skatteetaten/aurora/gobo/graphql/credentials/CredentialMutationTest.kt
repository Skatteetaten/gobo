package no.skatteetaten.aurora.gobo.graphql.credentials

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithoutDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirst
import no.skatteetaten.aurora.gobo.graphql.isFalse
import no.skatteetaten.aurora.gobo.graphql.isTrue
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerResult
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import org.springframework.security.test.context.support.WithMockUser

@Import(CredentialMutation::class)
abstract class CredentialMutationTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/mutations/registerPostgresMotelServer.graphql")
    protected lateinit var registerPostgresMotelMutation: Resource

    @MockkBean
    protected lateinit var herkimerService: HerkimerService

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
            .graphqlData("registerPostgresMotelServer.message")
            .isEqualTo("PostgresMotel host=test0oup could not be registered. The AuroraPlattform has internal configuration issues.")
    }
}

class UnauthorizedTokenCredentialMutation : CredentialMutationTest() {
    @Test
    fun `Mutate database schema return true given response success`() {
        webTestClient.queryGraphQL(
            queryResource = registerPostgresMotelMutation,
            variables = registerPostgresVariables,
            token = "test-token"
        ).expectStatus().isOk
            .expectBody()
            .graphqlErrorsFirst("message").isEqualTo("You do not have access to register a Postgres Motel Server")
    }
}
