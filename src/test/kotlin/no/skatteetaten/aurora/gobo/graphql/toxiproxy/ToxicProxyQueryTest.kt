package no.skatteetaten.aurora.gobo.graphql.toxiproxy

import com.ninjasquad.springmockk.MockkBean
import io.fabric8.openshift.api.model.User
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ApplicationDeploymentResourceBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.applicationdeployment.ApplicationDeploymentQuery
import no.skatteetaten.aurora.gobo.graphql.printResult
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationService
import no.skatteetaten.aurora.kubernetes.KubernetesCoroutinesClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.core.io.Resource

@Import(
    ApplicationDeploymentQuery::class,
    ToxiProxyDataLoader::class
)
class ToxicProxyQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getApplicationDeploymentWithToxics.graphql")
    private lateinit var getApplicationDeploymentWithToxicsQuery: Resource

    @MockkBean
    private lateinit var applicationService: ApplicationService

    @MockkBean(relaxed = true)
    private lateinit var kubernetesClient: KubernetesCoroutinesClient

    @BeforeEach
    fun setUp() {
        coEvery { kubernetesClient.currentUser(any()) } returns User()
    }

    @Test
    fun `Query for applications with toxics`() {
        coEvery { applicationService.getApplicationDeployment(any<String>()) } returns ApplicationDeploymentResourceBuilder().build()

        webTestClient.queryGraphQL(getApplicationDeploymentWithToxicsQuery, variables = mapOf("id" to "abc"), token = "test-token")
            .expectStatus().isOk
            .expectBody()
            .printResult()
/*
            .graphqlDataWithPrefix("affiliations.edges") {
                graphqlData("[0].node.name").isEqualTo("paas")
                graphqlData("[1].node.name").isEqualTo("demo")
                graphqlData("[2].node.name").isEqualTo("notDeployed")
*/
    }
}
