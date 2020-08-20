package no.skatteetaten.aurora.gobo.resolvers.auroraapimetadata

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.integration.boober.AuroraApiMetadataService
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Disabled
class AuroraApiMetadataResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getMetadata.graphql")
    private lateinit var query: Resource

    @MockkBean
    private lateinit var applicationService: AuroraApiMetadataService

    val configNames: List<String> = listOf("A", "B", "C")

    @BeforeEach
    fun setUp() {
        every { applicationService.getConfigNames() } returns configNames
        every { applicationService.getClientConfig() } returns ClientConfig(
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
            .graphqlDataWithPrefix("auroraApiMetadata") {
                graphqlData("configNames[0]").isEqualTo(configNames[0])
                graphqlData("clientConfig.openshiftCluster").isEqualTo("utv")
            }
            .graphqlDoesNotContainErrors()
    }
}
