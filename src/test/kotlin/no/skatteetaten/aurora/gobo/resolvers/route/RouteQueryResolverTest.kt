package no.skatteetaten.aurora.gobo.resolvers.route

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.ProgressionBuilder
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import no.skatteetaten.aurora.gobo.resolvers.GraphQLTestWithDbhAndSkap
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.graphqlDoesNotContainErrors
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class RouteQueryResolverTest : GraphQLTestWithDbhAndSkap() {

    @Value("classpath:graphql/queries/getRoute.graphql")
    private lateinit var getRoute: Resource

    @MockkBean
    private lateinit var routeService: RouteService

    @Test
    fun `get progressions for app`() {

        val job = ProgressionBuilder().build()
        every { routeService.getProgressions("namespace", "name") } returns listOf(job)

        webTestClient.queryGraphQL(
            queryResource = getRoute,
            variables = mapOf("namespace" to "namespace", "name" to "name"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlData("route.progressions[0].namespace").isEqualTo("namespace")
            .graphqlDoesNotContainErrors()
    }
}
