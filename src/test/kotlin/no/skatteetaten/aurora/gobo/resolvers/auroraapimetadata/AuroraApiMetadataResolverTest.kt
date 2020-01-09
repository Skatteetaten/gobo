package no.skatteetaten.aurora.gobo.resolvers.auroraapimetadata

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.integration.boober.AuroraApiMetadataService
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.AbstractGraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class AuroraApiMetadataResolverTest : AbstractGraphQLTest() {

    @Value("classpath:graphql/queries/getConfigNames.graphql")
    private lateinit var getApplicationsQuery: Resource

    @MockkBean
    private lateinit var applicationService: AuroraApiMetadataService

    @BeforeEach
    fun setUp() {
        val configNames: List<String> = listOf("A", "B", "C")
        every { applicationService.getConfigNames() } returns configNames
        every { applicationService.getClientConfig()} returns ClientConfig("", " ", " ", "")
    }

    @Test
    fun `Query for config names`() {
        val variables: Map<String, *> = emptyMap<String, String>()
        webTestClient.queryGraphQL(getApplicationsQuery, variables, null)
            .expectStatus().isOk
            // .expectBody()
            // .graphqlDataWithPrefix("auroraApiMetadata") {
            //     graphqlData("configNames").isArray()
          //  }
    }
}