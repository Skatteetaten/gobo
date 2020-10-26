package no.skatteetaten.aurora.gobo.graphql

import io.mockk.clearAllMocks
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

const val PROFILE_WITH_DBH_AND_SKAP = "with-dbh-and-skap"

@WithMockUser
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["graphql.packages=no.skatteetaten.aurora.gobo.graphql"]
)
abstract class GraphQLTestWithoutDbhAndSkap {
    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @AfterEach
    fun shutdown() = clearAllMocks()
}

@ActiveProfiles(PROFILE_WITH_DBH_AND_SKAP)
abstract class GraphQLTestWithDbhAndSkap : GraphQLTestWithoutDbhAndSkap()
