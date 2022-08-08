package no.skatteetaten.aurora.gobo.graphql.cname

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.CnameContentBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.spotless.SpotlessCnameService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(CnameContentQuery::class)
class CnameContentQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getCnameContent.graphql")
    private lateinit var getCnameContentQuery: Resource

    @Value("classpath:graphql/queries/getCnameContentWithAffiliation.graphql")
    private lateinit var getCnameContentQueryWithAffiliation: Resource

    @MockkBean
    private lateinit var spotlessCnameService: SpotlessCnameService

    @BeforeEach
    fun setup() {
        coEvery { spotlessCnameService.getCnameContent() } returns listOf(CnameContentBuilder().build())
    }

    @Test
    fun `Get cname content`() {
        webTestClient.queryGraphQL(getCnameContentQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cnameContent[0]") {
                graphqlData("canonicalName").isEqualTo("demo.localhost.no")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("ttlInSeconds").isEqualTo(3098)
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get cname content with affiliation`() {
        webTestClient.queryGraphQL(getCnameContentQueryWithAffiliation, mapOf("affiliation" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cnameContent[0]") {
                graphqlData("canonicalName").isEqualTo("demo.localhost.no")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("ttlInSeconds").isEqualTo(3098)
            }
            .graphqlDoesNotContainErrors()
    }
}
