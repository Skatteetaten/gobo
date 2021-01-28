package no.skatteetaten.aurora.gobo.graphql.credentials

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithoutDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.printResult
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerResult
import no.skatteetaten.aurora.gobo.integration.herkimer.HerkimerService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(CredentialMutation::class)
class CredentialMutationTest : GraphQLTestWithoutDbhAndSkap() {

    @Value("classpath:graphql/mutations/registerPostgresMotelServer.graphql")
    private lateinit var registerPostgresMotelMutation: Resource

    @MockkBean
    private lateinit var herkimerService: HerkimerService

    @Test
    fun `Mutate database schema return true given response success`() {
        val registerPostgresVariables = mapOf(
            "input" to jacksonObjectMapper().convertValue<Map<String, Any>>(
                PostgresMotelInput("test0oup", "username", "password", "bg")
            )
        )
        coEvery { herkimerService.registerResourceAndClaim(any()) } returns HerkimerResult(true)
        webTestClient.queryGraphQL(
            queryResource = registerPostgresMotelMutation,
            variables = registerPostgresVariables,
            token = "test-token"
        )
    }
}
