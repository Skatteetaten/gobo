package no.skatteetaten.aurora.gobo.resolvers.unclematt

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.ProbeResultListBuilder
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.resolvers.scan.ScanStatus
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.test.web.reactive.server.WebTestClient

@GraphQLTest
class ScanQueryResolverTest {

    @Value("classpath:graphql/queries/scan.graphql")
    private lateinit var scanQuery: Resource

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var probeService: ProbeServiceBlocking

    @Test
    fun `resolve scan response`() {
        given(
            probeService.probeFirewall(
                host = "test.server.no",
                port = 80
            )
        ).willReturn(ProbeResultListBuilder().build())

        val variables = mapOf("host" to "test.server.no", "port" to 80)
        webTestClient.queryGraphQL(scanQuery, variables)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("scan") {
                graphqlData("status").isEqualTo(ScanStatus.CLOSED.name)
                graphqlData("hostName").isNotEmpty
                graphqlData("port").isNumber
            }
            .graphqlDataWithPrefix("scan.failed") {
                graphqlData("totalCount").isNumber
                graphqlData("edges").isArray
                graphqlData("edges[0].node.status").isEqualTo(ScanStatus.CLOSED.name)
                graphqlData("edges[1].node.status").isEqualTo(ScanStatus.UNKNOWN.name)
                graphqlData("edges[0].node.resolvedIp").isNotEmpty
            }
            .graphqlDataWithPrefix("scan.open") {
                graphqlData("totalCount").isNumber
                graphqlData("edges").isArray
                graphqlData("edges[0].node.status").isEqualTo(ScanStatus.OPEN.name)
                graphqlData("edges[0].node.resolvedIp").isNotEmpty
            }
    }
}
