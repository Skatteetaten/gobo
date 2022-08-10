package no.skatteetaten.aurora.gobo.graphql.cname

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.coVerify
import no.skatteetaten.aurora.gobo.CnameAzureBuilder
import no.skatteetaten.aurora.gobo.CnameInfoBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.IntegrationDisabledException
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsFirstContainsMessage
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

    @Value("classpath:graphql/queries/getCnameAzureOnPrem.graphql")
    private lateinit var getCnameAzureOnPrem: Resource

    @Value("classpath:graphql/queries/getCnameAzureOnPremForAffiliation.graphql")
    private lateinit var getCnameAzureOnPremForAffiliation: Resource

    @Value("classpath:graphql/queries/getCnameAzure.graphql")
    private lateinit var getCnameAzure: Resource

    @Value("classpath:graphql/queries/getCnameOnPrem.graphql")
    private lateinit var getCnameOnPrem: Resource

    @Value("classpath:graphql/queries/getCnameAzureForAffiliation.graphql")
    private lateinit var getCnameAzureForAffiliation: Resource

    @Value("classpath:graphql/queries/getCnameOnPremForAffiliation.graphql")
    private lateinit var getCnameOnPremForAffiliation: Resource

    @MockkBean
    private lateinit var cnameService: CnameService

    @MockkBean
    private lateinit var spotlessCnameService: SpotlessCnameService

    @BeforeEach
    fun setup() {
        coEvery { cnameService.getCnameInfo() } returns listOf(
            CnameInfoBuilder().build(),
            CnameInfoBuilder("test-demo").build()
        )
        coEvery { spotlessCnameService.getCnameContent(listOf("aurora")) } returns listOf(CnameAzureBuilder().build())
        coEvery { spotlessCnameService.getCnameContent(null) } returns listOf(
            CnameAzureBuilder("test-demo").build(),
            CnameAzureBuilder("test-utv").build()
        )
    }

    @Test
    fun `Get cname of azure and onPrem`() {
        webTestClient.queryGraphQL(getCnameAzureOnPrem)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname.azure[0]") {
                graphqlData("canonicalName").isEqualTo("demo.localhost.no")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("ttlInSeconds").isEqualTo(3098)
                graphqlData("namespace").isEqualTo("test-demo")
            }
            .graphqlDataWithPrefix("cname.onPrem[0]") {
                graphqlData("status").isEqualTo("SUCCESS")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("appName").isEqualTo("demo")
            }
            .graphqlDoesNotContainErrors()

        coVerify { spotlessCnameService.getCnameContent(null) }
    }

    @Test
    fun `Get cname of azure and onPrem for affiliation`() {
        webTestClient.queryGraphQL(getCnameAzureOnPremForAffiliation, mapOf("affiliation" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname.azure[0]") {
                graphqlData("canonicalName").isEqualTo("demo.localhost.no")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("ttlInSeconds").isEqualTo(3098)
            }
            .graphqlDataWithPrefix("cname.onPrem[0]") {
                graphqlData("status").isEqualTo("SUCCESS")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("appName").isEqualTo("demo")
            }
            .graphqlDataWithPrefix("cname") {
                graphqlData("azure[1]").doesNotExist()
                graphqlData("onPrem[1]").doesNotExist()
            }
            .graphqlDoesNotContainErrors()
        coVerify { spotlessCnameService.getCnameContent(listOf("aurora")) }
    }

    @Test
    fun `Get cname of azure and failing onPrem`() {
        coEvery { cnameService.getCnameInfo() } throws IntegrationDisabledException("CnameService integration disabled")

        webTestClient.queryGraphQL(getCnameAzureOnPrem)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname.azure[0]") {
                graphqlData("canonicalName").isEqualTo("demo.localhost.no")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("ttlInSeconds").isEqualTo(3098)
            }
            .graphqlDataWithPrefix("cname") {
                graphqlData("onPrem").isEmpty
            }
            .graphqlErrorsFirstContainsMessage("CnameService integration disabled")
    }

    @Test
    fun `Get cname of azure`() {
        webTestClient.queryGraphQL(getCnameAzure)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname.azure[0]") {
                graphqlData("canonicalName").isEqualTo("demo.localhost.no")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("ttlInSeconds").isEqualTo(3098)
            }
            .graphqlDataWithPrefix("cname") {
                graphqlData("onPrem").doesNotExist()
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get cname of onPrem`() {
        webTestClient.queryGraphQL(getCnameOnPrem)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname.onPrem[0]") {
                graphqlData("status").isEqualTo("SUCCESS")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("appName").isEqualTo("demo")
            }
            .graphqlDataWithPrefix("cname") {
                graphqlData("azure").doesNotExist()
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Get cname of azure for affiliation`() {
        webTestClient.queryGraphQL(getCnameAzureForAffiliation, mapOf("affiliation" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname.azure[0]") {
                graphqlData("canonicalName").isEqualTo("demo.localhost.no")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("ttlInSeconds").isEqualTo(3098)
            }
            .graphqlDataWithPrefix("cname") {
                graphqlData("azure[1]").doesNotExist()
                graphqlData("onPrem").doesNotExist()
            }
            .graphqlDoesNotContainErrors()

        coVerify { spotlessCnameService.getCnameContent(listOf("aurora")) }
    }

    @Test
    fun `Get cname of onPrem for affiliation`() {
        webTestClient.queryGraphQL(getCnameOnPremForAffiliation, mapOf("affiliation" to "aurora"))
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("cname.onPrem[0]") {
                graphqlData("status").isEqualTo("SUCCESS")
                graphqlData("clusterId").isEqualTo("utv")
                graphqlData("appName").isEqualTo("demo")
            }
            .graphqlDataWithPrefix("cname") {
                graphqlData("azure").doesNotExist()
                graphqlData("onPrem[1]").doesNotExist()
            }
            .graphqlDoesNotContainErrors()
    }
}
