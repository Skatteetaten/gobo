package no.skatteetaten.aurora.gobo.resolvers.applicationdeployment

import com.fasterxml.jackson.databind.node.TextNode
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
import no.skatteetaten.aurora.gobo.service.ApplicationUpgradeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class DeployMutationResolverTest : AbstractGraphQLTest() {
    @Value("classpath:graphql/mutations/deploy.graphql")
    private lateinit var deployMutation: Resource

    @MockkBean
    private lateinit var applicationDeploymentService: ApplicationDeploymentService

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
                deploymentSpec = TextNode("adf")

            )
        )

    )

    @BeforeEach
    fun setUp() {
        every { applicationDeploymentService.deploy(any(), any(), any(), any()) } returns result
    }

    /*
    @Test
    fun `Mutate application deployment version`() {
        val variables = mapOf(
            "input" to mapOf(
                "applicationDeploymentId" to "123",
                "version" to "1"

            )
        )
        webTestClient.queryGraphQL(redeployWithVersionMutation, variables).expectBody()
            .graphqlData("redeployWithVersion").isNotEmpty
    }

    */
}
