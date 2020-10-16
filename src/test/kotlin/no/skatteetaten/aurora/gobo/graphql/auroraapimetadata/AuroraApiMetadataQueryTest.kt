package no.skatteetaten.aurora.gobo.graphql.auroraapimetadata

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.printResult
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.AuroraApiMetadataService
import no.skatteetaten.aurora.gobo.integration.boober.ConfigNames
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class AuroraApiMetadataQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getMetadata.graphql")
    private lateinit var query: Resource

    @MockkBean
    private lateinit var applicationService: AuroraApiMetadataService

    val configNames: List<String> = listOf("A", "B", "C")

    @BeforeEach
    fun setUp() {
        coEvery { applicationService.getConfigNames() } returns ConfigNames(configNames)
        coEvery { applicationService.getClientConfig() } returns ClientConfig(
            "foo.bar/%s/",
            "utv",
            "http://utv.cluster:8443",
            2
        )
    }

    @Test
    fun `Query for config names`() {
        webTestClient.queryGraphQL(query)
            .expectStatus().isOk
            .expectBody()
            .printResult()
            /*
            .graphqlDataWithPrefix("auroraApiMetadata") {
                graphqlData("configNames[0]").isEqualTo(configNames[0])
                graphqlData("clientConfig.openshiftCluster").isEqualTo("utv")
            }
            .graphqlDoesNotContainErrors()
             */
    }
}