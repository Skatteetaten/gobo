package no.skatteetaten.aurora.gobo.resolvers.job

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.JobResourceBuilder
import no.skatteetaten.aurora.gobo.integration.skap.JobService
import no.skatteetaten.aurora.gobo.resolvers.AbstractGraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlData
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

class JobQueryResolverTest : AbstractGraphQLTest() {

    @Value("classpath:graphql/queries/getJobs.graphql")
    private lateinit var getJobs: Resource

    @MockkBean
    private lateinit var jobService: JobService

    @Test
    fun `get jobs for app`() {

        val job = JobResourceBuilder().build()
        every { jobService.getJobs("namespace", "name") } returns listOf(job)

        webTestClient.queryGraphQL(
            queryResource = getJobs,
            variables = mapOf("namespace" to "namespace", "name" to "name"),
            token = "test-token"
        )
            .expectStatus().isOk
            .expectBody()
            .graphqlData("jobs[0].namespace").isEqualTo("namespace")
    }
}
