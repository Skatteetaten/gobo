package no.skatteetaten.aurora.gobo.resolvers.webseal

import com.nhaarman.mockito_kotlin.any
import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.OpenShiftUserBuilder
import no.skatteetaten.aurora.gobo.WebsealStateBuilder
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.security.OpenShiftUserLoader
import no.skatteetaten.aurora.gobo.service.WebsealAffiliationService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class WebsealStateResolverTest {
    @Value("classpath:graphql/queries/getWebsealStates.graphql")
    private lateinit var getWebsealStates: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var openShiftUserLoader: OpenShiftUserLoader

    @MockBean
    private lateinit var websealAffiliationService: WebsealAffiliationService

    @BeforeEach
    fun setUp() {
        given(openShiftUserLoader.findOpenShiftUserByToken(anyString()))
            .willReturn(OpenShiftUserBuilder().build())
    }

    @AfterEach
    fun tearDown() = reset(openShiftUserLoader)

    @Test
    fun `Get WebSEAL states`() {
        given(websealAffiliationService.getWebsealState(any())).willReturn(listOf(WebsealStateBuilder().build()))

        webTestClient.queryGraphQL(
            queryResource = getWebsealStates,
            variables = mapOf("affiliation" to "aurora"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations.edges[0].node.websealStates[0]") {
                it.graphqlData("name").isEqualTo("test.no")
                it.graphqlData("acl.aclName").isEqualTo("acl-name")
                it.graphqlData("junctions[0].id").isEqualTo("junction-id")
            }
    }
}