package no.skatteetaten.aurora.gobo.graphql.cname

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.CnameAzureBuilder
import no.skatteetaten.aurora.gobo.CnameInfoBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.gavel.CnameService
import no.skatteetaten.aurora.gobo.integration.spotless.SpotlessCnameService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(CnameQuery::class)
class CnameQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getCnameOnPrem.graphql")
    private lateinit var getCnameOnPremQuery: Resource

    @Value("classpath:graphql/queries/getCnameOnPremWithAffiliation.graphql")
    private lateinit var getCnameOnPremQueryWithAffiliation: Resource

    @Value("classpath:graphql/queries/getCnameAzure.graphql")
    private lateinit var getCnameAzureQuery: Resource

    @Value("classpath:graphql/queries/getCnameAzureWithAffiliation.graphql")
    private lateinit var getCnameAzureQueryWithAffiliation: Resource

    @MockkBean
    private lateinit var spotlessCnameService: SpotlessCnameService

    @MockkBean
    private lateinit var cnameService: CnameService

    @BeforeEach
    fun setup() {
        coEvery { spotlessCnameService.getCnameContent() } returns listOf(CnameAzureBuilder().build())
        coEvery { cnameService.getCnameInfo() } returns listOf(CnameInfoBuilder().build())
    }

    @Test
    fun `Get cname onPrem`() {
        webTestClient.queryGraphQL(getCnameOnPremQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname[0]") {
                graphqlData("status").isEqualTo("SUCCESS")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("appName").isEqualTo("demo")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get cname onPrem with affiliation`() {
        webTestClient.queryGraphQL(getCnameOnPremQueryWithAffiliation, mapOf("affiliation" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname[0]") {
                graphqlData("status").isEqualTo("SUCCESS")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("appName").isEqualTo("demo")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get cname azure`() {
        webTestClient.queryGraphQL(getCnameAzureQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname[0]") {
                graphqlData("canonicalName").isEqualTo("demo.localhost.no")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("ttlInSeconds").isEqualTo(3098)
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get cname azure with affiliation`() {
        webTestClient.queryGraphQL(getCnameAzureQueryWithAffiliation, mapOf("affiliation" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname[0]") {
                graphqlData("canonicalName").isEqualTo("demo.localhost.no")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("ttlInSeconds").isEqualTo(3098)
            }
            .graphqlDoesNotContainErrors()
    }
}
