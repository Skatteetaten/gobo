package no.skatteetaten.aurora.gobo.resolvers.route

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.ProgressionResourceBuilder
import no.skatteetaten.aurora.gobo.integration.skap.RouteService
import no.skatteetaten.aurora.gobo.integration.skap.Routes
import no.skatteetaten.aurora.gobo.resolvers.AbstractGraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class RoutesQueryResolverTest : AbstractGraphQLTest() {

    @Value("classpath:graphql/queries/getJobs.graphql")
    private lateinit var getRoutes: Resource

    @MockkBean
    private lateinit var routeService: RouteService

    @Test
    fun `get progressions for app`() {

        val job = ProgressionResourceBuilder().build()
        every { routeService.getProgressions("namespace", "name") } returns Routes(listOf(job))

        webTestClient.queryGraphQL(
            queryResource = getRoutes,
            variables = mapOf("namespace" to "namespace", "name" to "name"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlData("routes.progressions[0].namespace").isEqualTo("namespace")
    }
}
