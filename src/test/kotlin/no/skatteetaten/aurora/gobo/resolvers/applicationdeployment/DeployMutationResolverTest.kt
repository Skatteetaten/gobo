package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.integration.Response
import no.skatteetaten.aurora.gobo.integration.boober.ApplicationDeploymentService
import no.skatteetaten.aurora.gobo.integration.boober.AuroraConfigRefResource
import no.skatteetaten.aurora.gobo.integration.boober.DeployResource
import no.skatteetaten.aurora.gobo.resolvers.AbstractGraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.isTrue
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class DeployMutationResolverTest : AbstractGraphQLTest() {
    @Value("classpath:graphql/mutations/deploy.graphql")
    private lateinit var deployMutation: Resource

    @MockkBean
    private lateinit var applicationDeploymentService: ApplicationDeploymentService

    var deploymentSpecAsJson = "{" +
        " \"cluster\" : {\"value\" : \"hei\"}, " +
        " \"envName\" : {\"value\" : \"env\"}, " +
        " \"name\" : {\"value\" : \"myName\"}, " +
        " \"version\" : {\"value\" : \"1.0\"} " +
        "}"
    val deploymentSpec: JsonNode = jacksonObjectMapper().readTree(deploymentSpecAsJson)

    val result = Response<DeployResource>(
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
                success = true
            )
        )
    )

    @BeforeEach
    fun setUp() {
        every { applicationDeploymentService.deploy(any(), any(), any(), any()) } returns result
    }

    @Test
    fun `Mutate application deployment version`() {
        val variables = mapOf(
            "input" to mapOf(
                "auroraConfigName" to "a/b/c",
                "auroraConfigReference" to "feature/abc",
                "applicationDeployment" to emptyList<ApplicationDeploymentRef>()
            )
        )
        webTestClient.queryGraphQL(deployMutation, variables)
            .expectStatus().isOk
            .expectBody()
            .graphqlData("deploy.success").isTrue()
            .graphqlData("deploy.auroraConfigRef.gitReference").isEqualTo("master")
            .graphqlData("deploy.auroraConfigRef.commitId").isEqualTo("123abcd")
            .graphqlData("deploy.applicationDeployments[0].version").isEqualTo("1.0")
            .graphqlData("deploy.applicationDeployments[0].cluster").isEqualTo("hei")
    }
}
