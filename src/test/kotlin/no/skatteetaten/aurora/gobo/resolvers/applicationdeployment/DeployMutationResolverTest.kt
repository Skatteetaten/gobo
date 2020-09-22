package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigRefResource
import no.skatteetaten.aurora.gobo.integration.boober.DeployResource
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.isFalse
import no.skatteetaten.aurora.gobo.resolvers.isTrue
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class DeployMutationResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/mutations/deploy.graphql")
    private lateinit var deployMutation: Resource

    @MockkBean
    private lateinit var applicationDeploymentService: ApplicationDeploymentService

    @BeforeEach
    fun setUp() {
        val deploymentSpecAsJson =
            """{
                "cluster" : {"value" : "myCluster"},  
                "envName" : {"value" : "env"},  
                "name"    : {"value" : "myName"},  
                "version" : {"value" : "1.0"}
            }"""

        val deploymentSpec: JsonNode = jacksonObjectMapper().readTree(deploymentSpecAsJson)

        val result = Response(
            success = true,
            message = "YIHA",
            items = listOf(
                DeployResource(
                    auroraConfigRef = AuroraConfigRefResource(
                        name = "demo",
                        refName = "master",
                        resolvedRef = "123abcd"
                    ),
                    deploymentSpec = deploymentSpec,
                    deployId = "myID",
                    openShiftResponses = emptyList(),
                    success = true,
                    applicationDeploymentId = "abc123"
                )
            )
        )

        coEvery { applicationDeploymentService.deploy("myToken2", any(), any(), any()) } returns result

        val resultFail = Response(
            success = false,
            message = "error",
            items = listOf(
                DeployResource(
                    auroraConfigRef = AuroraConfigRefResource(
                        name = "myName",
                        refName = "myRef",
                        resolvedRef = "123abcd"
                    ),
                    deploymentSpec = deploymentSpec,
                    deployId = "oneId",
                    openShiftResponses = emptyList(),
                    success = false,
                    applicationDeploymentId = "abc123"
                )
            )
        )
        coEvery { applicationDeploymentService.deploy("myToken", any(), any(), any()) } returns resultFail
    }

    @Test
    fun `deploy application`() {
        val variables = mapOf(
            "input" to mapOf(
                "auroraConfigName" to "a/b/c",
                "auroraConfigReference" to "feature/abc",
                "applicationDeployment" to emptyList<ApplicationDeploymentRef>()
            )
        )

        webTestClient.queryGraphQL(deployMutation, variables, "myToken2")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("deploy.success").isTrue()
            .graphqlData("deploy.auroraConfigRef.gitReference").isEqualTo("master")
            .graphqlData("deploy.auroraConfigRef.commitId").isEqualTo("123abcd")
            .graphqlData("deploy.applicationDeployments[0].spec.version").isEqualTo("1.0")
            .graphqlData("deploy.applicationDeployments[0].spec.cluster").isEqualTo("myCluster")
            .graphqlData("deploy.applicationDeployments[0].applicationDeploymentId").isEqualTo("abc123")
            .graphqlDoesNotContainErrors()
    }

    @Test
    fun `deploy fail`() {
        val variables = mapOf(
            "input" to mapOf(
                "auroraConfigName" to "b/d/e",
                "auroraConfigReference" to "feature/1337",
                "applicationDeployment" to emptyList<ApplicationDeploymentRef>()
            )
        )

        webTestClient.queryGraphQL(deployMutation, variables, "myToken")
            .expectStatus().isOk
            .expectBody()
            .graphqlData("deploy.success").isFalse()
            .graphqlData("deploy.auroraConfigRef.gitReference").isEqualTo("myRef")
            .graphqlData("deploy.auroraConfigRef.commitId").isEqualTo("123abcd")
            .graphqlData("deploy.applicationDeployments[0].spec.version").isEqualTo("1.0")
            .graphqlData("deploy.applicationDeployments[0].spec.cluster").isEqualTo("myCluster")
            .graphqlDoesNotContainErrors()
    }
}
