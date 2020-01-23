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

class CreateAuroraConfigFileTest : AbstractGraphQLTest() {

    val token = "test-token"

    @Value("classpath:graphql/mutations/createAuroraConfigFile.graphql")
    private lateinit var query: Resource

    @MockkBean
    private lateinit var auroraConfigService: AuroraConfigService

    @BeforeEach
    fun setUp() {
        every {
            auroraConfigService.addAuroraConfigFile(any(), any(), any(), any(), any())
        } returns Response(
            success = true,
            message = "Ok",
            items = listOf(
                ChangedAuroraConfigFileResponse(
                    errors = emptyList(), file = AuroraConfigFileResource(
                        name = "myFileName",
                        contents = "myContent",
                        type = DEFAULT,
                        contentHash = "myHash"
                    )
                )
            ),
            count = 1
        )
    }

    @Test
    fun `Create Aurora config file`() {
        val variables = mapOf(
            "input" to mapOf(
                "auroraConfigName" to "newFile",
                "fileName" to "kxxxxx/newFile.yaml",
                "contents" to "{  \"version\" : \"1338\" }",
                "auroraConfigReference" to "myRef"
            )
        )

        webTestClient.queryGraphQL(query, variables, token)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("createAuroraConfigFile") {
                graphqlData("success").isEqualTo("true")
                graphqlData("file.contents").isEqualTo("myContent")
            }
    }
}
