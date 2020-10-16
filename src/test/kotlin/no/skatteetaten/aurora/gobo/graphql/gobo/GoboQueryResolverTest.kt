package no.skatteetaten.aurora.gobo.graphql.gobo

import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Disabled
class GoboQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getGoboUsage.graphql")
    private lateinit var getGoboUsageQuery: Resource

    @Value("classpath:graphql/queries/getGoboUsageFieldNameContains.graphql")
    private lateinit var getGoboUsageFieldNameContainsQuery: Resource

    @Value("classpath:graphql/queries/getGoboUserUsage.graphql")
    private lateinit var getGoboUserUsageQuery: Resource

    @Test
    fun `Get Gobo usage`() {
        webTestClient.queryGraphQL(getGoboUsageQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlData("gobo.startTime").isNotEmpty
            .graphqlDataWithPrefix("gobo.usage.usedFields[0]") {
                graphqlData("name").isNotEmpty
                graphqlData("count").isNotEmpty
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get Gobo usage with field name containing`() {
        webTestClient.queryGraphQL(
            queryResource = getGoboUsageFieldNameContainsQuery,
            variables = mapOf("nameContains" to "gobo")
        )
            .expectStatus().isOk
    }

    @Test
    fun `Get Gobo user usage`() {
        webTestClient.queryGraphQL(getGoboUserUsageQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("gobo.usage.users[0]") {
                graphqlData("name").isEqualTo("anonymous")
                graphqlData("count").isNumber
            }
            .graphqlDoesNotContainErrors()
    }
}