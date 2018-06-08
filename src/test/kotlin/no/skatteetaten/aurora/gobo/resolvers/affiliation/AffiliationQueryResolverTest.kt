package no.skatteetaten.aurora.gobo.resolvers.affiliation

import no.skatteetaten.aurora.gobo.affiliation.AffiliationService
import no.skatteetaten.aurora.gobo.application.ApplicationService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AffiliationQueryResolverTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var affiliationService: AffiliationService

    @MockBean
    private lateinit var applicationService: ApplicationService

    private val query =
        """{"query":"{\n  affiliations {\n    totalCount\n    edges {\n      node {\n        name\n      }\n    }\n  }\n}","variables":{"affiliations":["paas","pasd"]},"operationName":null}"""

    @Test
    fun `Query for all affiliations`() {
        webTestClient
            .post()
            .uri("/graphql")
            .body(BodyInserters.fromObject(query))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.affiliations.totalCount").isNumber
    }
}