package no.skatteetaten.aurora.gobo.resolvers.auroraconfig

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileResource
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType.APP
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigFileType.GLOBAL
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigService
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class AuroraConfigQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getFile.graphql")
    private lateinit var query: Resource

    @MockkBean
    private lateinit var auroraConfigService: AuroraConfigService

    @MockkBean
    private lateinit var applicationDeploymentService: ApplicationDeploymentService

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

        every { applicationDeploymentService.getSpec(any(), any(), any(), any()) } returns listOf(
            ApplicationDeploymentSpec(jacksonObjectMapper().readTree(jsonNode))
        )
    }

    @Test
    fun `Query for application deployment`() {
        val variables = mapOf(
            "auroraConfig" to "demo",
            "fileName" to "about.json",
            "applicationDeplymentRef" to mapOf("environment" to "my-env", "application" to "my-application")
        )
        webTestClient.queryGraphQL(query, variables, "test-token")
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
}
