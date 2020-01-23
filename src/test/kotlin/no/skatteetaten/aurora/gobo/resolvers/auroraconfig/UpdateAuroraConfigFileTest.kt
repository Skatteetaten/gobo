package no.skatteetaten.aurora.gobo.resolvers.auroraconfig

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType.DEFAULT
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.resolvers.AbstractGraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class UpdateAuroraConfigFileTest : AbstractGraphQLTest() {

    val token = "test-token"

    @Value("classpath:graphql/mutations/updateAuroraConfigFile.graphql")
    private lateinit var query: Resource

    @MockkBean
    private lateinit var auroraConfigService: AuroraConfigService

    @BeforeEach
    fun setUp() {
        every {
            auroraConfigService.updateAuroraConfigFile(any(), any(), any(), any(), any(), any())
        } returns Response(
            success = true,
            message = "Ok",
            items = listOf(
                ChangedAuroraConfigFileResponse(
                    errors = emptyList(), file = AuroraConfigFileResource(
                        name = "my name",
                        contents = "my content",
                        type = DEFAULT,
                        contentHash = "my hash"
                    )
                )
            ),
            count = 1
        )
    }

    @Test
    fun `Update Aurora config file`() {
        val variables = mapOf(
            "input" to mapOf(
                "auroraConfigName" to "demo",
                "fileName" to "kxxxxx/referanse.yaml",
                "contents" to "{  \"version\" : \"1337\" }",
                "existingHash" to "1337abc7331"
            )
        )

        webTestClient.queryGraphQL(query, variables, token)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("updateAuroraConfigFile") {
                graphqlData("success").isEqualTo("true")
                graphqlData("file.contentHash").isEqualTo("my hash")
            }
    }
}
