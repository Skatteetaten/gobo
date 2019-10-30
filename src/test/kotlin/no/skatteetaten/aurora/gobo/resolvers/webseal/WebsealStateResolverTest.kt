package no.skatteetaten.aurora.gobo.resolvers.webseal

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.WebsealStateResourceBuilder
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class WebsealStateResolverTest {
    @Value("classpath:graphql/queries/getWebsealStates.graphql")
    private lateinit var getWebsealStates: Resource

    @Value("classpath:graphql/queries/getWebsealStatesWithPropertyNames.graphql")
    private lateinit var getWebsealStatesWithPropertyNames: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @MockkBean
    private lateinit var websealAffiliationService: WebsealAffiliationService

    @BeforeEach
    fun setUp() {
        every { openShiftUserLoader.findOpenShiftUserByToken(any()) } returns OpenShiftUserBuilder().build()
    }

    @AfterEach
    fun tearDown() = clearAllMocks()

    @Test
    fun `Get WebSEAL states`() {
        every { websealAffiliationService.getWebsealState(any()) } returns
            mapOf(
                "aurora" to listOf(WebsealStateResourceBuilder().build())
            )

        webTestClient.queryGraphQL(
            queryResource = getWebsealStates,
            variables = mapOf("affiliation" to "aurora"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges[0].node.websealStates[0]") {
                graphqlData("name").isEqualTo("test.no")
                graphqlData("acl.aclName").isEqualTo("acl-name")
                graphqlData("junctions.length()").isEqualTo(2)
            }
    }

    @Test
    fun `Get WebSEAL states with property names`() {
        every { websealAffiliationService.getWebsealState(any()) } returns
            mapOf(
                "aurora" to listOf(WebsealStateResourceBuilder().build())
            )

        webTestClient.queryGraphQL(
            queryResource = getWebsealStatesWithPropertyNames,
            variables = mapOf("affiliation" to "aurora"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges[0].node.websealStates[0]") {
                graphqlData("junctions.length()").isEqualTo(2)
            }
    }
}
