package no.skatteetaten.aurora.gobo.graphql.gobo

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.infrastructure.client.Client
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldClient
import no.skatteetaten.aurora.gobo.infrastructure.field.Field
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.infrastructure.client.ClientService
import no.skatteetaten.aurora.gobo.infrastructure.field.FieldService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(
    GoboQuery::class,
    GoboFieldUsageBatchDataLoader::class,
    GoboClientBatchDataLoader::class,
    GoboFieldCountBatchDataLoader::class,
    GoboClientCountBatchDataLoader::class
)
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

    @Value("classpath:graphql/queries/getGoboClientUsageNameContains.graphql")
    private lateinit var getGoboClientUsageNameContainsQuery: Resource

    @Value("classpath:graphql/queries/getGoboUsageMostUsed.graphql")
    private lateinit var getGoboMostUsedFieldsQuery: Resource

    @BeforeEach
    internal fun setUp() {
        val field = Field("gobo", 5, listOf(FieldClient("donald", 2), FieldClient("joe", 3)))
        every { fieldService.getAllFields() } returns listOf(field)
        every { fieldService.getFieldWithName(any()) } returns listOf(field)
        every { fieldService.getFieldCount() } returns 1

        val client = Client("donald", 2)
        every { clientService.getAllClients() } returns listOf(client)
        every { clientService.getClientWithName(any()) } returns listOf(client)
    }

    @Test
    fun `Get Gobo usage`() {
        webTestClient.queryGraphQL(queryResource = getGoboUsageQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("gobo.startTime").isNotEmpty
            .graphqlData("gobo.usage.numberOfFields").isNumber
            .graphqlData("gobo.usage.numberOfClients").isNumber
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
            .graphqlDataWithPrefix("gobo.usage.clients[0]") {
                graphqlData("name").isNotEmpty
                graphqlData("count").isNumber
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get Gobo usage with client name containing`() {
        webTestClient.queryGraphQL(
            queryResource = getGoboClientUsageNameContainsQuery,
            variables = mapOf("nameContains" to "donald"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("gobo.usage.clients[0]") {
                graphqlData("name").isEqualTo("donald")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get most used fields`() {
        webTestClient.queryGraphQL(
            queryResource = getGoboMostUsedFieldsQuery,
            variables = mapOf("mostUsedOnly" to true),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlData("gobo.usage.usedFields[0].name").isEqualTo("gobo")
            .graphqlDoesNotContainErrors()
    }
}
