package no.skatteetaten.aurora.gobo.resolvers

import io.mockk.clearAllMocks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration

@WithMockUser
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["graphql.packages=no.skatteetaten.aurora.gobo.resolvers"]
)
abstract class GraphQLTestWithoutDbhAndSkap {
    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @AfterEach
    fun shutdown() = clearAllMocks()

    @BeforeEach
    fun setUpInitial() {
        webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(30)).build()
    }
}

@ActiveProfiles("with-dbh-and-skap")
abstract class GraphQLTestWithDbhAndSkap : GraphQLTestWithoutDbhAndSkap()
