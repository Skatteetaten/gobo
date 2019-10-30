package no.skatteetaten.aurora.gobo.resolvers.affiliation

import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.AffiliationServiceBlocking
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.resolvers.AbstractGraphQLTest
import no.skatteetaten.aurora.gobo.resolvers.graphqlDataWithPrefix
import no.skatteetaten.aurora.gobo.resolvers.queryGraphQL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.util.Base64Utils

class AffiliationQueryResolverTest : AbstractGraphQLTest() {

    @Value("classpath:graphql/queries/getAffiliations.graphql")
    private lateinit var getAffiliationsQuery: Resource

    @MockkBean
    private lateinit var affiliationService: AffiliationServiceBlocking

    @MockkBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @AfterEach
    fun tearDown() = clearAllMocks()

    @Test
    fun `Query for all affiliations`() {
        val affiliations = listOf("paas", "demo")
        every { affiliationService.getAllAffiliations() } returns affiliations
        every { applicationService.getApplications(affiliations) } returns listOf(ApplicationResourceBuilder().build())

        webTestClient.queryGraphQL(getAffiliationsQuery)
            .expectStatus().isOk
            .expectBody()
            .graphqlDataWithPrefix("affiliations") {
                graphqlData("totalCount").isEqualTo(2)
                graphqlData("edges[0].node.name").isEqualTo("paas")
                graphqlData("edges[0].cursor").isEqualTo("paas".toBase64())
                graphqlData("edges[1].node.name").isEqualTo("demo")
                graphqlData("edges[1].cursor").isEqualTo("demo".toBase64())
            }
    }

    private fun String.toBase64() = Base64Utils.encodeToString(this.toByteArray())
}
