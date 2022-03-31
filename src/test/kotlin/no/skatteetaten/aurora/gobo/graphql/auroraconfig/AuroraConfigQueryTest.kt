package no.skatteetaten.aurora.gobo.graphql.auroraconfig

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.graphqlErrorsMissingToken
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType.APP
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType.GLOBAL
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService

@Import(AuroraConfigQuery::class, ApplicationDeploymentSpecDataLoader::class, ApplicationFilesResourceDataLoader::class)
class AuroraConfigQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getFile.graphql")
    private lateinit var query: Resource

    @Value("classpath:graphql/queries/getApplicationFiles.graphql")
    private lateinit var getApplicationFilesQuery: Resource

    @MockkBean
    private lateinit var auroraConfigService: AuroraConfigService

    @MockkBean
    private lateinit var applicationDeploymentService: ApplicationDeploymentService

    @BeforeEach
    fun setUp() {
        coEvery { auroraConfigService.getAuroraConfig(any(), any(), any()) } returns AuroraConfig(
            name = "demo",
            ref = "master",
            resolvedRef = "abcde",
            files = listOf(
                AuroraConfigFileResource("about.json", """{ "foo" : "bar" }""", GLOBAL, "123"),
                AuroraConfigFileResource("utv/foo.json", """{ "foo" : "bar" }""", APP, "321")
            )
        )

        val jsonNode =
            """
          {
            "cluster": {
              "value": "myCluster"
            },
            "envName": {
              "value": "myEnvName"
            },
            "name": {
              "value": "myName"
            },
            "version": {
              "value": "myVersion"
            },
            "releaseTo": {
              "value": "myReleaseTo"
            },
            "type": {
              "value": "myType"
            },
            "pause": {
               "value": false
            },
            "deployStrategy": {
                "type" : {
                  "value": "myDeploy"
             }           
            },
            "replicas": {
              "value": 2
            }
          }
            """.trimIndent()

        coEvery { applicationDeploymentService.getSpec(any(), any(), any(), any()) } returns listOf(
            ApplicationDeploymentSpec(jacksonObjectMapper().readTree(jsonNode))
        )

        coEvery {
            auroraConfigService.getAuroraConfigFiles(
                any(),
                "demo",
                "my-env",
                "my-application",
                any()
            )
        } returns listOf(
            AuroraConfigFileResource("about.json", """{ "foo" : "bar" }""", GLOBAL, "123"),
            AuroraConfigFileResource("utv/foo.json", """{ "foo" : "bar" }""", APP, "321")
        )
    }

    private val input = mapOf(
        "auroraConfig" to "demo",
        "fileNames" to "about.json",
        "applicationDeploymentRefInput" to mapOf("environment" to "my-env", "application" to "my-application")
    )

    private val applicationFilesInput = mapOf(
        "auroraConfig" to "demo",
        "applicationDeploymentRefInput" to mapOf("environment" to "my-env", "application" to "my-application")
    )

    @Test
    fun `Query for application deployment`() {
        webTestClient.queryGraphQL(query, input, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("auroraConfig") {
                graphqlData("resolvedRef").isEqualTo("abcde")
                graphqlData("files.length()").isEqualTo(1)
                graphqlData("files[0].name").isEqualTo("about.json")
                graphqlData("applicationDeploymentSpec[0].cluster").isEqualTo("myCluster")
                graphqlData("applicationDeploymentSpec[0].replicas").isEqualTo("2")
                graphqlData("applicationDeploymentSpec[0].paused").isEqualTo(false)
                graphqlData("applicationDeploymentSpec[0].releaseTo").doesNotExist()
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query for application files`() {
        webTestClient.queryGraphQL(getApplicationFilesQuery, applicationFilesInput, "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("auroraConfig") {
                graphqlData("applicationFiles[0].applicationDeploymentRef.application").isEqualTo("my-application")
                graphqlData("applicationFiles[0].applicationDeploymentRef.environment").isEqualTo("my-env")
                graphqlData("applicationFiles[0].files.length()").isEqualTo("1")
                graphqlData("applicationFiles[0].files[0].name").isEqualTo("utv/foo.json")
                graphqlData("applicationFiles[0].files[0].contents").isEqualTo("""{ "foo" : "bar" }""")
                graphqlData("applicationFiles[0].files[0].contentHash").isEqualTo("321")
            }
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `Query without token`() {
        webTestClient.queryGraphQL(query, input)
            .expectStatus().isOk
            .expectBody()
            .graphqlErrorsMissingToken()
    }
}
