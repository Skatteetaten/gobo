package no.skatteetaten.aurora.gobo.graphql.gobo

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.domain.ClientService
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.ClientDto
import no.skatteetaten.aurora.gobo.domain.model.FieldClientDto
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.printResult
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(GoboQuery::class, FieldService::class)
class GoboQueryTest : GraphQLTestWithDbhAndSkap() {

    @MockkBean
    private lateinit var fieldService: FieldService

    @MockkBean
    private lateinit var clientService: ClientService

    @Value("classpath:graphql/queries/getGoboUsage.graphql")
    private lateinit var getGoboUsageQuery: Resource

    @Value("classpath:graphql/queries/getGoboUsageFieldNameContains.graphql")
    private lateinit var getGoboUsageFieldNameContainsQuery: Resource

    @Value("classpath:graphql/queries/getGoboClientUsage.graphql")
    private lateinit var getGoboClientUsageQuery: Resource

    @BeforeEach
    internal fun setUp() {
        every { fieldService.getAllFields() } returns listOf(
            FieldDto(
                "gobo",
                5,
                listOf(FieldClientDto("donald", 2), FieldClientDto("joe", 3))
            )
        )

        every { clientService.getAllClients() } returns listOf(ClientDto("donald", 2))
    }

    @Test
    fun `Get Gobo usage`() {
        webTestClient.queryGraphQL(queryResource = getGoboUsageQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("gobo.startTime").isNotEmpty
            .graphqlDataWithPrefix("gobo.usage.usedFields[0]") {
                graphqlData("name").isNotEmpty
                graphqlData("count").isNotEmpty
                graphqlData("clients").isArray
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get Gobo usage with field name containing`() {
        webTestClient.queryGraphQL(
            queryResource = getGoboUsageFieldNameContainsQuery,
            variables = mapOf("nameContains" to "gobo"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("gobo.usage.usedFields[0]") {
                graphqlData("name").isEqualTo("gobo")
            }
    }

    @Test
    fun `Get Gobo client usage`() {
        webTestClient.queryGraphQL(queryResource = getGoboClientUsageQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .printResult()
            /*
            .graphqlDataWithPrefix("gobo.usage.clients[0]") {
                graphqlData("name").isNotEmpty
                graphqlData("count").isNumber
            }
            .graphqlDoesNotContainErrors()

             */
    }
}
