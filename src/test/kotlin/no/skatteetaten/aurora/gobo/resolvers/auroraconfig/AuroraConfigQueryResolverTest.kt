package no.skatteetaten.aurora.gobo.resolvers.auroraconfig

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType.APP
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType.GLOBAL
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.resolvers.AbstractGraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class AuroraConfigQueryResolverTest : AbstractGraphQLTest() {

    @Value("classpath:graphql/queries/getFile.graphql")
    private lateinit var query: Resource

    @MockkBean
    private lateinit var auroraConfigService: AuroraConfigService

    @BeforeEach
    fun setUp() {
        every { auroraConfigService.getAuroraConfig(any(), any(), any()) } returns AuroraConfig(
            name = "demo",
            ref = "master",
            resolvedRef = "abcde",
            files = listOf(
                AuroraConfigFileResource("about.json", """{ "foo" : "bar" }""", GLOBAL, "123"),
                AuroraConfigFileResource("utv/foo.json", """{ "foo" : "bar" }""", APP, "321")
            )
        )

        every {
            auroraConfigService.addAuroraConfigFile(any(), any(), any(), any(), any()) } returns Response(
            success = true,
            count = 1,
            message = "Ok",
            items = emptyList()
        )
    }

    @Test
    fun `Query for application deployment`() {
        val variables = mapOf("auroraConfig" to "demo", "fileName" to "about.json")
        webTestClient.queryGraphQL(query, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("auroraConfig") {
                graphqlData("resolvedRef").isEqualTo("abcde")
                graphqlData("files.length()").isEqualTo(1)
                graphqlData("files[0].name").isEqualTo("about.json")
            }
    }
}
