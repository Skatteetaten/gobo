package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationService
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import reactor.core.publisher.Mono

class AffiliationQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getAffiliations.graphql")
    private lateinit var getAffiliationsQuery: Resource

    @Value("classpath:graphql/queries/getAffiliationsWithVisibilityCheck.graphql")
    private lateinit var getAffiliationsWithVisibilityQuery: Resource

    @Value("classpath:graphql/queries/getAffiliation.graphql")
    private lateinit var getAffiliationQuery: Resource

    @MockkBean
    private lateinit var affiliationService: AffiliationService

    @AfterEach
    fun tearDown() = clearAllMocks()

    @Test
    fun `Query for all affiliations`() {
        val affiliations = listOf("paas", "demo")
        every { affiliationService.getAllAffiliations() } returns Mono.just(affiliations) // FIXME mono warning

        webTestClient.queryGraphQL(getAffiliationsQuery, token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.items") {
                graphqlData("[0].name").isEqualTo("paas")
                graphqlData("[1].name").isEqualTo("demo")
            }
            .graphqlData("affiliations.totalCount").isEqualTo(2)
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for visible affiliations`() {
        val affiliations = listOf("paas", "demo")
        every { affiliationService.getAllVisibleAffiliations("test-token") } returns Mono.just(affiliations) // FIXME mono warning

        webTestClient.queryGraphQL(getAffiliationsWithVisibilityQuery, mapOf("checkForVisibility" to true), "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.items") {
                graphqlData("[0].name").isEqualTo("paas")
                graphqlData("[1].name").isEqualTo("demo")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for affiliation`() {
        webTestClient.queryGraphQL(getAffiliationQuery, mapOf("affiliation" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .graphqlData("affiliations.items[0].name").isEqualTo("aurora")
            .graphqlData("affiliations.totalCount").isEqualTo(1)
            .graphqlDoesNotContainErrors()

    }
}
