package no.skatteetaten.aurora.gobo.resolvers.webseal

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.WebsealStateResourceBuilder
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class WebsealStateResolverTest : GraphQLTestWithDbhAndSkap() {
    @Value("classpath:graphql/queries/getWebsealStates.graphql")
    private lateinit var getWebsealStates: Resource

    @Value("classpath:graphql/queries/getWebsealStatesWithPropertyNames.graphql")
    private lateinit var getWebsealStatesWithPropertyNames: Resource

    @MockkBean
    private lateinit var websealAffiliationService: WebsealAffiliationService

    @Test
    fun `Get WebSEAL states`() {
        every { websealAffiliationService.getWebsealState(any()) } returns
            mapOf(
                "aurora" to listOf(WebsealStateResourceBuilder().build())
            )

        webTestClient.queryGraphQL(
            queryResource = getWebsealStates,
            variables = mapOf("affiliation" to "aurora"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges[0].node.websealStates[0]") {
                graphqlData("name").isEqualTo("test.no")
                graphqlData("acl.aclName").isEqualTo("acl-name")
                graphqlData("junctions.length()").isEqualTo(2)
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get WebSEAL states with property names`() {
        every { websealAffiliationService.getWebsealState(any()) } returns
            mapOf(
                "aurora" to listOf(WebsealStateResourceBuilder().build())
            )

        webTestClient.queryGraphQL(
            queryResource = getWebsealStatesWithPropertyNames,
            variables = mapOf("affiliation" to "aurora"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges[0].node.websealStates[0]") {
                graphqlData("junctions.length()").isEqualTo(2)
            }
            .graphqlDoesNotContainErrors()
    }
}
