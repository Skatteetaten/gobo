package no.skatteetaten.aurora.gobo.graphql.cname

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.CnameInfoBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.gavel.CnameService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(CnameInfoQuery::class)
class CnameInfoQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getCnameInfo.graphql")
    private lateinit var getCnameInfoQuery: Resource

    @Value("classpath:graphql/queries/getCnameInfoWithAffiliation.graphql")
    private lateinit var getCnameInfoQueryWithAffiliation: Resource

    @MockkBean
    private lateinit var cnameService: CnameService

    @BeforeEach
    fun setUp() {
        coEvery { cnameService.getCnameInfo() } returns listOf(CnameInfoBuilder().build())
    }

    @Test
    fun `Get cname info`() {
        webTestClient.queryGraphQL(getCnameInfoQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cnameInfo[0]") {
                graphqlData("status").isEqualTo("SUCCESS")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("appName").isEqualTo("demo")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get cname info with affiliation`() {
        webTestClient.queryGraphQL(getCnameInfoQueryWithAffiliation, mapOf("affiliation" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cnameInfo[0]") {
                graphqlData("status").isEqualTo("SUCCESS")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("appName").isEqualTo("demo")
            }
            .graphqlDoesNotContainErrors()
    }
}
