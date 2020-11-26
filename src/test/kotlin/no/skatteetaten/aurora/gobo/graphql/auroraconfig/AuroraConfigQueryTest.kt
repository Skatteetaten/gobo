package no.skatteetaten.aurora.gobo.graphql.auroraconfig

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(AuroraConfigQuery::class, ApplicationDeploymentSpecListDataLoader::class)
class AuroraConfigQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getFile.graphql")
    private lateinit var query: Resource

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
    }

    private val input = mapOf(
        "auroraConfig" to "demo",
        "fileNames" to "about.json",
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
    fun `Query without token`() {
        webTestClient.queryGraphQL(query, input)
            .expectStatus().isOk
            .expectBody()
            .graphqlErrorsMissingToken()
    }
}
