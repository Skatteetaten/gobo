package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService

@Import(AuroraConfigQuery::class, ApplicationDeploymentSpecDataLoader::class)
class ApplicationAuroraConfigFilesQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationFiles.graphql")
    private lateinit var query: Resource

    @MockkBean
    private lateinit var auroraConfigService: AuroraConfigService

    @MockkBean
    private lateinit var applicationDeploymentService: ApplicationDeploymentService

    @BeforeEach
    fun setUp() {
        coEvery { auroraConfigService.getApplicationAuroraConfigFiles(any(), any(), any(), any()) } returns listOf(
            AuroraConfigFileResource("about.json", """{ "foo" : "bar" }""", AuroraConfigFileType.GLOBAL, "123"),
            AuroraConfigFileResource("utv/foo.json", """{ "foo" : "bar" }""", AuroraConfigFileType.APP, "321")
        )
    }

    @Test
    fun `Query for application deployment`() {

        val input = mapOf(
            "name" to "demo",
            "application" to "app",
            "environment" to "env"
        )

        webTestClient.queryGraphQL(query, input, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("applicationAuroraConfigFiles[0]") {
                graphqlData("name").isEqualTo("about.json")
                graphqlData("contents").isEqualTo("""{ "foo" : "bar" }""")
                graphqlData("contentHash").isEqualTo("123")
                graphqlData("type").isEqualTo("GLOBAL")
            }.graphqlDataWithPrefix("applicationAuroraConfigFiles[1]") {
                graphqlData("name").isEqualTo("utv/foo.json")
                graphqlData("contents").isEqualTo("""{ "foo" : "bar" }""")
                graphqlData("contentHash").isEqualTo("321")
                graphqlData("type").isEqualTo("APP")
            }
            .graphqlDoesNotContainErrors()
    }
}
