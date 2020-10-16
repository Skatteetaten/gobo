package no.skatteetaten.aurora.gobo.graphql.route

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.SkapJobForBigipBuilder
import no.skatteetaten.aurora.gobo.SkapJobForWebsealBuilder
import no.skatteetaten.aurora.gobo.graphql.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.graphql.graphqlData
import no.skatteetaten.aurora.gobo.graphql.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.graphql.queryGraphQL
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class RouteQueryTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getRoute.graphql")
    private lateinit var getRoute: Resource

    @MockkBean
    private lateinit var routeService: RouteService

    @Test
    fun `get jobs for app`() {
        val websealjob = SkapJobForWebsealBuilder().build()
        val bigipJob = SkapJobForBigipBuilder().build()
        every { routeService.getSkapJobs("namespace", "name-webseal") } returns listOf(websealjob)
        every { routeService.getSkapJobs("namespace", "name-bigip") } returns listOf(bigipJob)

        webTestClient.queryGraphQL(
            queryResource = getRoute,
            variables = mapOf("namespace" to "namespace", "name" to "name"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlData("route.websealJobs[0].id").isEqualTo("75745")
            .graphqlData("route.websealJobs[0].host").isEqualTo("testing.test.no")
            .graphqlData("route.bigipJobs[0].id").isEqualTo("465774")
            .graphqlData("route.bigipJobs[0].asmPolicy").isEqualTo("testing-get")
            .graphqlDoesNotContainErrors()
    }
}
