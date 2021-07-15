package no.skatteetaten.aurora.gobo.graphql.webseal

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.WebsealStateResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.affiliation.AffiliationQuery
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.service.AffiliationService
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(AffiliationQuery::class, WebsealStateBatchDataLoader::class)
class WebsealStateTest : GraphQLTestWithDbhAndSkap() {
    @Value("classpath:graphql/queries/getWebsealStates.graphql")
    private lateinit var getWebsealStates: Resource

    @Value("classpath:graphql/queries/getWebsealStatesWithPropertyNames.graphql")
    private lateinit var getWebsealStatesWithPropertyNames: Resource

    @MockkBean
    private lateinit var websealAffiliationService: WebsealAffiliationService

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @Test
    fun `Get WebSEAL states`() {
        coEvery { websealAffiliationService.getWebsealState(any()) } returns
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
        coEvery { websealAffiliationService.getWebsealState(any()) } returns
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
