package no.skatteetaten.aurora.gobo.graphql.gobo

import org.junit.jupiter.api.BeforeEach
import com.ninjasquad.springmockk.MockkBean
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import io.mockk.every
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldClientDto
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import org.junit.jupiter.api.Disabled

@Import(GoboQuery::class, FieldService::class)
class GoboQueryTest : GraphQLTestWithDbhAndSkap() {

    @MockkBean
    private lateinit var fieldService: FieldService

    @Value("classpath:graphql/queries/getGoboUsage.graphql")
    private lateinit var getGoboUsageQuery: Resource

    @Value("classpath:graphql/queries/getGoboUsageFieldNameContains.graphql")
    private lateinit var getGoboUsageFieldNameContainsQuery: Resource

    @Value("classpath:graphql/queries/getGoboUserUsage.graphql")
    private lateinit var getGoboUserUsageQuery: Resource

    @BeforeEach
    internal fun setUp() {
        every { fieldService.getAllFields() } returns listOf(
            FieldDto(
                "gobo",
                5,
                listOf(FieldClientDto("donald", 2), FieldClientDto("joe", 3))
            )
        )
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

    @Disabled("Implement client usage")
    @Test
    fun `Get Gobo user usage`() {
        webTestClient.queryGraphQL(queryResource = getGoboUserUsageQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("gobo.usage.users[0]") {
                graphqlData("name").isNotEmpty
                graphqlData("count").isNumber
            }
            .graphqlDoesNotContainErrors()
    }
}
