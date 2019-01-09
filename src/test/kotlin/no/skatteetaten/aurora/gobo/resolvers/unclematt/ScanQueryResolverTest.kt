package no.skatteetaten.aurora.gobo.resolvers.unclematt

import no.skatteetaten.aurora.gobo.GraphQLTest
import no.skatteetaten.aurora.gobo.ProbeResultListBuilder
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeServiceBlocking
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

    @Value("classpath:graphql/scan.graphql")
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
            .jsonPath("data.scan.status").isEqualTo(ScanStatus.CLOSED.name)
            .jsonPath("data.scan.hostName").isNotEmpty
            .jsonPath("data.scan.port").isNumber
            .jsonPath("data.scan.failed.totalCount").isNumber
            .jsonPath("data.scan.failed.edges").isArray
            .jsonPath("data.scan.failed.edges[0].node.status").isEqualTo(ScanStatus.CLOSED.name)
            .jsonPath("data.scan.failed.edges[1].node.status").isEqualTo(ScanStatus.UNKNOWN.name)
            .jsonPath("data.scan.failed.edges[0].node.resolvedIp").isNotEmpty
            .jsonPath("data.scan.open.totalCount").isNumber
            .jsonPath("data.scan.open.edges").isArray
            .jsonPath("data.scan.open.edges[0].node.status").isEqualTo(ScanStatus.OPEN.name)
            .jsonPath("data.scan.open.edges[0].node.resolvedIp").isNotEmpty
    }
}
