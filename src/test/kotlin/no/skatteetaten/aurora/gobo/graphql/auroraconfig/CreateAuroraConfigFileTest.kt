package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType.DEFAULT
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(AuroraConfigQuery::class, AuroraConfigMutation::class)
class CreateAuroraConfigFileTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/mutations/createAuroraConfigFile.graphql")
    private lateinit var query: Resource

    @MockkBean
    private lateinit var auroraConfigService: AuroraConfigService

    @BeforeEach
    fun setUp() {

        val auroraConfigFileResource = AuroraConfigFileResource(
            name = "myFileName",
            contents = "myContent",
            type = DEFAULT,
            contentHash = "myHash"
        )

        coEvery {
            auroraConfigService.addAuroraConfigFile(any(), any(), any(), any(), any())
        } returns auroraConfigFileResource
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

        webTestClient.queryGraphQL(query, variables, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("createAuroraConfigFile") {
                graphqlData("success").isEqualTo("true")
                graphqlData("file.contents").isEqualTo("myContent")
            }
            .graphqlDoesNotContainErrors()
    }
}
