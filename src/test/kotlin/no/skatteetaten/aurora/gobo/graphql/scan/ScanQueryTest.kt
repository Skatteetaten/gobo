package no.skatteetaten.aurora.gobo.graphql.scan

import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.skatteetaten.aurora.gobo.ProbeResultListBuilder
import no.skatteetaten.aurora.gobo.integration.unclematt.ProbeService
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class ScanQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/scan.graphql")
    private lateinit var scanQuery: Resource

    @MockkBean
    private lateinit var probeService: ProbeService

    @Suppress("ReactiveStreamsUnusedPublisher")
    @Test
    fun `resolve scan response`() {
        coEvery {
            probeService.probeFirewall("test.server.no", 80)
        } coAnswers {
            ProbeResultListBuilder().build()
        }

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
            .graphqlDoesNotContainErrors()
    }
}
