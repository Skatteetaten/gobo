package no.skatteetaten.aurora.gobo.graphql.toxiproxy

// @Import(DeployEnvironmentMutation::class)
/*
class AddToxiProxyToxicMutationTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/mutations/addToxiProxyToxic.graphql")
    private lateinit var addToxiProxyToxicMutation: Resource

    @MockkBean
    private lateinit var environmentService: EnvironmentService

    @Test
    fun `add toxic on existing toxi-proxy`() {
        coEvery { environmentService.deployEnvironment(any(), any()) } returns listOf(DeploymentResourceBuilder().build())

        webTestClient.queryGraphQL(deployEnvironmentMutation, DeploymentEnvironmentInput("dev-utv"), "test-token")
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("deployEnvironment.[0]") {
                graphqlData("deployId").isEqualTo("123")
                graphqlData("deploymentRef.cluster").isEqualTo("utv")
                graphqlData("deploymentRef.affiliation").isEqualTo("aurora")
                graphqlData("deploymentRef.environment").isEqualTo("dev-utv")
                graphqlData("deploymentRef.application").isEqualTo("gobo")
                graphqlData("timestamp").isNotEmpty
                graphqlData("message").isEmpty
            }
            .graphqlDoesNotContainErrors()
    }
}
*/
