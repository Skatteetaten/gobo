package no.skatteetaten.aurora.gobo.resolvers.unclematt

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.ProbeResultListBuilder
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.AbstractGraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import no.skatteetaten.aurora.gobo.resolvers.scan.ScanStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class ScanQueryResolverTest : AbstractGraphQLTest() {

    @Value("classpath:graphql/queries/scan.graphql")
    private lateinit var scanQuery: Resource

    @MockkBean
    private lateinit var probeService: ProbeServiceBlocking

    @Test
    fun `resolve scan response`() {
        every { probeService.probeFirewall("test.server.no", 80) } returns ProbeResultListBuilder().build()

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
